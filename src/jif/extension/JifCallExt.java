package jif.extension;

import java.util.ArrayList;
import java.util.List;

import jif.JifScheduler;
import jif.ast.JifUtil;
import jif.translate.ToJavaExt;
import jif.types.JifClassType;
import jif.types.JifContext;
import jif.types.JifFieldInstance;
import jif.types.JifMethodInstance;
import jif.types.JifParsedPolyType;
import jif.types.JifTypeSystem;
import jif.types.Param;
import jif.types.PathMap;
import jif.types.SemanticDetailedException;
import jif.types.label.AccessPathField;
import jif.types.label.Label;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Call;
import polyglot.ast.CallOps;
import polyglot.ast.Expr;
import polyglot.ast.Lang;
import polyglot.ast.Node;
import polyglot.ast.Receiver;
import polyglot.ast.Special;
import polyglot.frontend.FileSource;
import polyglot.frontend.Job;
import polyglot.frontend.MissingDependencyException;
import polyglot.frontend.goals.Goal;
import polyglot.types.FieldInstance;
import polyglot.types.MethodInstance;
import polyglot.types.ParsedClassType;
import polyglot.types.ReferenceType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.TypeChecker;

/** The Jif extension of the <code>Call</code> node.
 * 
 *  @see polyglot.ast.Call_c
 */
public class JifCallExt extends JifExprExt implements CallOps {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifCallExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Type findContainer(TypeSystem ts, MethodInstance mi) {
        Type container = mi.container();
        if (container instanceof JifParsedPolyType) {
            JifParsedPolyType jppt = (JifParsedPolyType) container;
            if (jppt.params().size() > 0) {
                // return the "null instantiation" of the base type,
                // to ensure that all TypeNodes contain either
                // a JifParsedPolyType with zero params, or a
                // JifSubstClassType
                return ((JifTypeSystem) ts).nullInstantiate(node().position(),
                        jppt.instantiatedFrom());
            }
        }
        return superLang().findContainer(node(), ts, mi);
    }

    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        Call me = node();

        JifContext A = lc.jifContext();
        A = (JifContext) me.del().enterScope(A);
        JifTypeSystem ts = lc.jifTypeSystem();

        if (A.checkingInits()) {
            // in the constructor prologue, the this object cannot be the receiver or an argument
            if (me.target() instanceof Expr && JifUtil
                    .effectiveExpr((Expr) me.target()) instanceof Special) {
                throw new SemanticDetailedException(
                        "No methods may be called on \"this\" object in a constructor prologue.",
                        "In a constructor body before the call to the super class, no "
                                + "reference to the \"this\" object is allowed to escape. This means "
                                + "that no methods of the current object may be called.",
                        me.position());
            }
            for (Expr arg : me.arguments()) {
                if (JifUtil.effectiveExpr(arg) instanceof Special) {
                    throw new SemanticDetailedException(
                            "The \"this\" object cannot be used as a method argument in a constructor prologue.",
                            "In a constructor body before the call to the super class, no "
                                    + "reference to the \"this\" object is allowed to escape. This means "
                                    + "that the \"this\" object cannot be used as a method argument.",
                            arg.position());
                }

            }

        }

        List<Type> throwTypes = new ArrayList<Type>(me.del().throwTypes(ts));

        Receiver target = (Receiver) lc.context(A).labelCheck(me.target());

        ReferenceType rt = target.type().toReference();

        // TODO: Refactor and use JifUtil.processFAP instead.
        // XXX: Ideally this is not needed here, since the reachable final access paths should include these
        // If there are any final static labels/principals with initializers in rt,
        // add them to the env.
        if (rt != null && rt.fields() != null) {
            for (FieldInstance fi : rt.fields()) {
                JifFieldInstance jfi = (JifFieldInstance) fi;
                if (jfi.flags().isFinal() && jfi.flags().isStatic()
                        && jfi.hasInitializer()) {
                    AccessPathField path = (AccessPathField) ts
                            .varInstanceToAccessPath(jfi, jfi.position());
                    Param init = jfi.initializer();
                    if (ts.isLabel(jfi.type())) {
                        Label dl = ts.dynamicLabel(jfi.position(), path);
                        Label rhs_label = (Label) init;
                        if (rhs_label == null) {
                            // label checking has not been done on ct yet
                            JifScheduler sched = (JifScheduler) lc.job()
                                    .extensionInfo().scheduler();
                            ParsedClassType pct = (ParsedClassType) rt;
                            if (sched.sourceHasJob(pct.fromSource())) {
                                Job job = sched.loadSource(
                                        (FileSource) pct.fromSource(), true);
                                if (job != null) {
                                    Goal g = sched.LabelsDoubleChecked(job);
                                    throw new MissingDependencyException(g);
                                }
                            }
                            // Turns out label checking has occurred, but the init was null.
                            // Just skip it.
                            continue;
                        }
                        A.addDefinitionalAssertionEquiv(dl, rhs_label, true);
                    } else if (ts.isImplicitCastValid(jfi.type(),
                            ts.Principal())) {
                        DynamicPrincipal dp =
                                ts.dynamicPrincipal(jfi.position(), path);
                        Principal rhs_principal = (Principal) init;
                        if (rhs_principal == null) {
                            // label checking has not been done on ct yet
                            JifScheduler sched = (JifScheduler) lc.job()
                                    .extensionInfo().scheduler();
                            ParsedClassType pct = (ParsedClassType) rt;

                            if (sched.sourceHasJob(pct.fromSource())) {
                                Job job = sched.loadSource(
                                        (FileSource) pct.fromSource(), true);
                                if (job != null) {
                                    Goal g = sched.LabelsDoubleChecked(job);
                                    throw new MissingDependencyException(g);
                                }
                            }
                            // turns out label checking has occurred, but the init was null.
                            rhs_principal = ts.bottomPrincipal(
                                    Position.compilerGenerated());
                        }
                        A.addDefinitionalEquiv(dp, rhs_principal);
                    }
                }
            }
        }

        // Find the method instance again. This ensures that
        // we have the correctly instantiated type, as label checking
        // of the target may have produced a new type for the target.
        JifMethodInstance mi = (JifMethodInstance) ts.findMethod(rt, me.name(),
                me.methodInstance().formalTypes(), A.currentClass());

        me = me.methodInstance(mi);

        if (mi.flags().isStatic()) {
            new ConstructorChecker().checkStaticMethodAuthority(mi, A, lc,
                    me.position());
        }

        A = (JifContext) A.pushBlock();

        boolean npExc = false;
        Label objLabel = null;

        if (target instanceof Expr) {
            Expr e = (Expr) target;

            if (e.type() == null)
                throw new InternalCompilerError("Type of " + e + " is null",
                        e.position());

            PathMap Xs = getPathMap(target);
            updateContextPostTarget(lc, A, Xs);

            if (!(target instanceof Special)) {
                // a NPE may be thrown depending on the target.
                npExc = (!((JifCallDel) node().del()).targetIsNeverNull());
                objLabel = Xs.NV();
                updateContextPostTargetExpr(lc, A, Xs);
            } else {
                objLabel = ((JifClassType) lc.context().currentClass())
                        .thisLabel();
            }
        }

        CallHelper helper = lc.createCallHelper(objLabel, target,
                mi.container(), mi, me.arguments(), node().position());
        LabelChecker callLC = lc.context(A);
        me = helper.checkCall(callLC, throwTypes, me, npExc);

        // now use the call helper to bind the var labels that were created
        // during type checking of the call (see JifCallDel#typeCheck)
        JifCallDel del = (JifCallDel) me.del();
        helper.bindVarLabels(callLC, del.receiverVarLabel, del.argVarLabels,
                del.paramVarLabels);

        A = (JifContext) A.pop();

        //subst arguments of inst_type
        if (helper.returnType() != me.type()) {
            me = (Call) me.type(helper.returnType());
        }

        checkThrowTypes(throwTypes);
        return updatePathMap(
                me.target(target).arguments(helper.labelCheckedArgs()),
                helper.X());
    }

    /**
     * Utility method for updating the context after checking the target.
     *
     * Useful for overriding in projects like fabric.
     */
    protected void updateContextPostTarget(LabelChecker lc, JifContext A,
        PathMap Xtarg) {
        // At this point, the environment A should have been extended
        // to include any declarations of s.  Reset the PC label.
        A.setPc(Xtarg.N(), lc);
    }

    /**
     * Utility method for updating the context after checking the target and it
     * is an expression (not a Special node).
     *
     * Useful for overriding in projects like fabric.
     */
    protected void updateContextPostTargetExpr(LabelChecker lc, JifContext A,
        PathMap Xtarg) {
        // a NPE may be thrown depending on the target.
        A.setPc(Xtarg.NV(), lc);
    }

    @Override
    public Call node() {
        return (Call) super.node();
    }

    @Override
    public void printArgs(CodeWriter w, PrettyPrinter tr) {
        superLang().printArgs(node(), w, tr);
    }

    @Override
    public boolean constantValueSet(Lang lang) {
        return superLang().constantValueSet(node(), lang);
    }

    @Override
    public boolean isConstant(Lang lang) {
        return superLang().isConstant(node(), lang);
    }

    @Override
    public Object constantValue(Lang lang) {
        return superLang().constantValue(node(), lang);
    }

    @Override
    public ReferenceType findTargetType() throws SemanticException {
        return superLang().findTargetType(node());
    }

    @Override
    public Node typeCheckNullTarget(TypeChecker tc, List<Type> argTypes)
            throws SemanticException {
        return superLang().typeCheckNullTarget(node(), tc, argTypes);
    }
}
