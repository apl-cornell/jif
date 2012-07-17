package jif.extension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jif.JifScheduler;
import jif.ast.JifUtil;
import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.AccessPathField;
import jif.types.label.Label;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.frontend.FileSource;
import polyglot.frontend.Job;
import polyglot.frontend.MissingDependencyException;
import polyglot.frontend.goals.Goal;
import polyglot.types.MethodInstance;
import polyglot.types.ParsedClassType;
import polyglot.types.ReferenceType;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** The Jif extension of the <code>Call</code> node. 
 * 
 *  @see polyglot.ast.Call_c
 */
public class JifCallExt extends JifExprExt
{
    public JifCallExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheck(LabelChecker lc) throws SemanticException {
        Call me = (Call) node();

        JifContext A = lc.jifContext();
        A = (JifContext) me.del().enterScope(A);
        JifTypeSystem ts = lc.jifTypeSystem();

        if (A.checkingInits()) {
            // in the constructor prologue, the this object cannot be the receiver or an argument
            if (me.target() instanceof Expr && JifUtil.effectiveExpr((Expr)me.target()) instanceof Special) {
                throw new SemanticDetailedException("No methods may be called on \"this\" object in a constructor prologue.", 
                                                    "In a constructor body before the call to the super class, no " +
                                                    "reference to the \"this\" object is allowed to escape. This means " +
                                                    "that no methods of the current object may be called.", 
                                                    me.position());
            }
            for (Iterator iter = me.arguments().iterator(); iter.hasNext();) {
                Expr arg = (Expr)iter.next();
                if (JifUtil.effectiveExpr(arg) instanceof Special) {
                    throw new SemanticDetailedException("The \"this\" object cannot be used as a method argument in a constructor prologue.", 
                                                        "In a constructor body before the call to the super class, no " +
                                                        "reference to the \"this\" object is allowed to escape. This means " +
                                                        "that the \"this\" object cannot be used as a method argument.", 
                                                        arg.position());
                }

            }

        }

        List throwTypes = new ArrayList(me.del().throwTypes(ts));

        Receiver target = (Receiver) lc.context(A).labelCheck(me.target());
        
        ReferenceType rt = target.type().toReference();
        
        // TODO: Refactor and use JifUtil.processFAP instead.
        // XXX: Ideally this is not needed here, since the reachable final access paths should include these
        // If there are any final static labels/principals with initializers in rt,
        // add them to the env.
        if (rt != null && rt.fields() != null) {
            for (Iterator it = rt.fields().iterator(); it.hasNext();) {
                JifFieldInstance jfi = (JifFieldInstance) it.next();
                if (jfi.flags().isFinal() && jfi.flags().isStatic() && jfi.hasInitializer()) {
                    AccessPathField path = (AccessPathField) JifUtil.varInstanceToAccessPath(jfi, jfi.position());
                    Param init = jfi.initializer();
                    if (ts.isLabel(jfi.type())) {
                        Label dl = ts.dynamicLabel(jfi.position(), path);
                        Label rhs_label = (Label) init;
                        if (rhs_label == null) {
                            // label checking has not been done on ct yet
                            JifScheduler sched = (JifScheduler) lc.job().extensionInfo().scheduler();
                            ParsedClassType pct = (ParsedClassType) rt;
                            if(sched.sourceHasJob(pct.fromSource())) {
                                Job job = sched.loadSource((FileSource) pct.fromSource(),true);
                                if(job != null) {
                                    Goal g = sched.LabelsChecked(job);
                                    throw new MissingDependencyException(g);
                                }
                            }
                            // Turns out label checking has occurred, but the init was null.
                            // Just skip it.
                            continue;
                        }
                        A.addDefinitionalAssertionEquiv(dl, rhs_label, true);
                    } else if (ts.isImplicitCastValid(jfi.type(), ts.Principal())) {
                        DynamicPrincipal dp = ts.dynamicPrincipal(jfi.position(), path);                
                        Principal rhs_principal = (Principal) init;
                        if (rhs_principal == null) {
                            // label checking has not been done on ct yet
                            JifScheduler sched = (JifScheduler) lc.job().extensionInfo().scheduler();
                            ParsedClassType pct = (ParsedClassType) rt;
                            
                            if(sched.sourceHasJob(pct.fromSource())) {
                                Job job = sched.loadSource((FileSource) pct.fromSource(),true);
                                if(job != null) {
                                    Goal g = sched.LabelsChecked(job);
                                    throw new MissingDependencyException(g);
                                }
                            }
                            // turns out label checking has occurred, but the init was null.
                            rhs_principal = ts.bottomPrincipal(Position.compilerGenerated());
                        }
                        A.addDefinitionalEquiv(dp, rhs_principal);
                    }
                }
            }
        }
        

        // Find the method instance again. This ensures that
        // we have the correctly instantiated type, as label checking
        // of the target may have produced a new type for the target.
        JifMethodInstance mi = (JifMethodInstance)ts.findMethod(rt, 
                                          me.name(), 
                                          me.methodInstance().formalTypes(), 
                                          A.currentClass());

        me = me.methodInstance(mi);
        
        if (mi.flags().isStatic()) {
            new ConstructorChecker().checkStaticMethodAuthority(mi, A, lc, me.position());
        }


        A = (JifContext) A.pushBlock();

        boolean npExc = false;
        Label objLabel = null;

        if (target instanceof Expr) {
            Expr e = (Expr) target;

            if (e.type() == null) 
                throw new InternalCompilerError("Type of " + e + " is null", e.position());

            PathMap Xs = getPathMap(target);
            A.setPc(Xs.N(), lc);

            if (! (target instanceof Special)) {
                // a NPE may be thrown depending on the target.
                npExc = (!((JifCallDel)node().del()).targetIsNeverNull());
                objLabel = Xs.NV();
                A.setPc(Xs.NV(), lc);
            }
            else {
                objLabel = ((JifClassType) lc.context().currentClass()).thisLabel();
            }
        }

        CallHelper helper = lc.createCallHelper(objLabel, target, mi.container(), mi, me.arguments(), node().position());
        LabelChecker callLC = lc.context(A);
        helper.checkCall(callLC, throwTypes, npExc);

        // now use the call helper to bind the var labels that were created
        // during type checking of the call (see JifCallDel#typeCheck)
        JifCallDel del = (JifCallDel)me.del();
        helper.bindVarLabels(callLC, 
                             del.receiverVarLabel, 
                             del.argVarLabels, 
                             del.paramVarLabels);

        A = (JifContext) A.pop();

        //subst arguments of inst_type
        if (helper.returnType() != me.type()) {
            me = (Call) me.type(helper.returnType());
        }

        checkThrowTypes(throwTypes);
        return updatePathMap(me.target(target).arguments(helper.labelCheckedArgs()), helper.X());
    }
}
