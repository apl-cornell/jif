package jif.extension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jif.JifScheduler;
import jif.ast.JifUtil;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.AccessPathField;
import jif.types.label.Label;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.New;
import polyglot.ast.Node;
import polyglot.frontend.Job;
import polyglot.frontend.MissingDependencyException;
import polyglot.frontend.goals.Goal;
import polyglot.types.ClassType;
import polyglot.types.ParsedClassType;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;

/** The Jif extension of the <code>New</code> node. 
 * 
 *  @see polyglot.ast.New
 */
public class JifNewExt extends JifExprExt 
{
    public JifNewExt(ToJavaExt toJava) {
        super(toJava);
    }

    protected ConstructorChecker constructorChecker = new ConstructorChecker();

    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        New noe = (New) node();

        JifTypeSystem ts = lc.typeSystem();
        JifContext A = lc.jifContext();
        A = (JifContext) noe.del().enterScope(A);

        List throwTypes = new ArrayList(noe.del().throwTypes(ts));

        ClassType ct = (ClassType) ts.unlabel(noe.type());
        
        // If there are any final static labels/principals with initializers in rt,
        // add them to the env.
        if (ct != null && ct.fields() != null) {
            for (Iterator it = ct.fields().iterator(); it.hasNext();) {
                JifFieldInstance jfi = (JifFieldInstance) it.next();
                if (jfi.flags().isFinal() && jfi.flags().isStatic() && jfi.hasInitializer()) {
                    AccessPathField path = (AccessPathField) JifUtil.varInstanceToAccessPath(jfi, jfi.position());
                    Param init = jfi.initializer();
                    if (ts.isLabel(jfi.type())) {
                        Label dl = ts.dynamicLabel(jfi.position(), path);
                        Label rhs_label = (Label) init;
                        if (rhs_label == null) {
                            throw new InternalCompilerError("FinalParams has not run yet");
                            // label checking has not been done on ct yet
//                            JifScheduler sched = (JifScheduler) lc.job().extensionInfo().scheduler();
//                            ParsedClassType pct = (ParsedClassType) ct;
//                            Job job = pct.job();
//                            
//                            // first check if ct is in the same source file
//                            // if yes, then no point throwing a MDE - just set unreachableThisRun
//                            // so that label checking continues into ct
//                            Job currentJob = sched.currentJob();
//                            Goal subgoal = sched.LabelsChecked(pct.job());
//                            if (job.equals(currentJob)) {
//                                Goal g = sched.currentGoal();
//                                g.setUnreachableThisRun();
//                                sched.addDependencyAndEnqueue(g, subgoal, false);
//                                continue;
//                            } else {
//                                throw new MissingDependencyException(subgoal);
//                            }
                        }
                        A.addDefinitionalAssertionEquiv(dl, rhs_label, true);
                    } else if (ts.isImplicitCastValid(jfi.type(), ts.Principal())) {
                        DynamicPrincipal dp = ts.dynamicPrincipal(jfi.position(), path);                
                        Principal rhs_principal = (Principal) init;
                        if (rhs_principal == null) {
                            throw new InternalCompilerError("FinalParams has not run yet");
                            // label checking has not been done on ct yet
//                            JifScheduler sched = (JifScheduler) lc.job().extensionInfo().scheduler();
//                            ParsedClassType pct = (ParsedClassType) ct;
//                            Job job = pct.job();
//                            
//                            // first check if ct is in the same source file
//                            // if yes, then no point throwing a MDE - just set unreachableThisRun
//                            // so that label checking continues into ct
//                            Job currentJob = sched.currentJob();
//                            Goal subgoal = sched.LabelsChecked(pct.job());
//                            if (job.equals(currentJob)) {
//                                Goal g = sched.currentGoal();
//                                g.setUnreachableThisRun();
//                                sched.addDependencyAndEnqueue(g, subgoal, false);
//                                continue;
//                            } else {
//                                throw new MissingDependencyException(subgoal);
//                            }
                        }
                        A.addDefinitionalEquiv(dp, rhs_principal);
                    }
                }
            }
        }
        

        constructorChecker.checkConstructorAuthority(ct, A, lc, noe.position());

        Label newLabel = ts.freshLabelVariable(noe.position(), "new" + ct.name(),
                                               "label of the reference to the newly created " +
                                               ct.name() + " object, at " + noe.position());

        if (ts.isLabeled(noe.type()) ) {
            // error messages for equality constraints aren't displayed, so no
            // need to define error messages.  
            lc.constrain(new NamedLabel("new_label",
                                        "label of the reference to the newly created " + ct.name(), 
                                        newLabel), 
                        LabelConstraint.EQUAL, 
                        new NamedLabel("declared_label", 
                                       "declared label of the newly created " + ct.name(), 
                                       ts.labelOfType(noe.type())), 
                       A.labelEnv(),
                       noe.position());
        }
        CallHelper helper = lc.createCallHelper(newLabel, ct, 
                                           (JifProcedureInstance)noe.constructorInstance(), 
                                           noe.arguments(),
                                           node().position());

        helper.checkCall(lc, throwTypes, false);

        PathMap retX = helper.X();
        PathMap X = retX.NV(lc.upperBound(retX.NV(), newLabel));

        checkThrowTypes(throwTypes);
        return updatePathMap(noe.arguments(helper.labelCheckedArgs()), X);
    }
}
