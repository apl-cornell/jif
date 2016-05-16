package jif.extension;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jif.ast.JifExt_c;
import jif.translate.ToJavaExt;
import jif.types.ActsForConstraint;
import jif.types.ActsForParam;
import jif.types.Assertion;
import jif.types.AuthConstraint;
import jif.types.AutoEndorseConstraint;
import jif.types.CallerConstraint;
import jif.types.ConstraintMessage;
import jif.types.ExceptionPath;
import jif.types.JifClassType;
import jif.types.JifContext;
import jif.types.JifProcedureInstance;
import jif.types.JifTypeSystem;
import jif.types.LabelConstraint;
import jif.types.LabelLeAssertion;
import jif.types.LabelSubstitution;
import jif.types.NamedLabel;
import jif.types.Path;
import jif.types.PathMap;
import jif.types.PrincipalConstraint;
import jif.types.SemanticDetailedException;
import jif.types.label.Label;
import jif.types.label.NotTaken;
import jif.types.label.ProviderLabel;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Formal;
import polyglot.ast.ProcedureDecl;
import polyglot.main.Report;
import polyglot.types.ConstructorInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** The Jif extension of the <code>ProcedureDecl</code> node. 
 * 
 *  @see polyglot.ast.ProcedureDecl
 *  @see jif.types.JifProcedureInstance
 */
public class JifProcedureDeclExt_c extends JifExt_c
        implements JifProcedureDeclExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifProcedureDeclExt_c(ToJavaExt toJava) {
        super(toJava);
    }

    protected static String jif_verbose = "jif";

    /**
     * Label check the formals.
     * @throws SemanticException 
     */
    protected List<Formal> checkFormals(List<Formal> formals,
            JifProcedureInstance ci, LabelChecker lc) throws SemanticException {
        List<Formal> newFormals = new ArrayList<Formal>(formals.size());
        boolean changed = false;
        for (Formal formal : formals) {
            Formal newFormal = (Formal) lc.labelCheck(formal);
            if (newFormal != formal) changed = true;
            newFormals.add(newFormal);
        }
        if (changed) return newFormals;
        return formals;
    }

    /**
     * This methods corresponds to the check-arguments predicate in the
     * thesis (Figure 4.37).  It returns the start label of the method.
     * It mutates the local context (to the A'' in the rule).
     */
    protected Label checkEnforceSignature(JifProcedureInstance mi,
            LabelChecker lc) throws SemanticException {
        if (Report.should_report(jif_verbose, 2))
            Report.report(2, "Adding constraints for header of " + mi);

        JifContext A = lc.jifContext();

        // Set the "auth" variable.
        Set<Principal> newAuth = constrainAuth(mi, A);

        for (Principal p : newAuth) {
            // Check that there is a p' in the old "auth" set such that
            // p' actsFor p.
            checkActsForAuthority(p, A, lc);
        }

        addCallers(mi, newAuth);
        A.setAuthority(newAuth);

        constrainLabelEnv(mi, A, null);

        checkProviderAuthority(mi, lc);
        checkConstraintVariance(mi, lc);

        // check that any autoendorse constraints are satisfied,
        // and set and constrain the inital PC
        Label Li = checkAutoEndorseConstrainPC(mi, lc);

        return Li;
    }

    protected void checkProviderAuthority(JifProcedureInstance mi,
            LabelChecker lc) throws SemanticException {
        final JifContext A = lc.jifContext();
        final ProviderLabel provider = mi.provider();
        final NamedLabel namedProvider = new NamedLabel(provider.toString(),
                "provider of " + provider.classType().fullName(), provider);
        for (Assertion c : mi.constraints()) {
            if (c instanceof CallerConstraint) {
                final CallerConstraint cc = (CallerConstraint) c;

                for (final Principal pi : cc.principals()) {
                    // Check that the provider of the enclosing class acts for pi.
                    final JifProcedureInstance _mi = mi;
                    lc.constrain(namedProvider, pi, A.labelEnv(), cc.position(),
                            new ConstraintMessage() {
                                @Override
                                public String msg() {
                                    return provider + " must act for " + pi;
                                }

                                @Override
                                public String detailMsg() {
                                    return provider + " is the provider of "
                                            + _mi.container()
                                            + " but does not have authority to act for "
                                            + pi;
                                }
                            });
                }
            }
        }
    }

    protected Label checkAutoEndorseConstrainPC(JifProcedureInstance mi,
            LabelChecker lc) throws SemanticException {
        final JifContext A = lc.jifContext();
        JifTypeSystem ts = lc.jifTypeSystem();
        JifClassType ct = (JifClassType) A.currentClass();
        Label Li = mi.pcBound();
        Label endorseTo = ts.topLabel();

        for (Assertion c : mi.constraints()) {
            if (c instanceof AutoEndorseConstraint) {
                AutoEndorseConstraint ac = (AutoEndorseConstraint) c;
                endorseTo = ts.meet(endorseTo, ac.endorseTo());
            }
        }

        Label callerPcLabel = ts.callSitePCLabel(mi);
        if (!mi.flags().isStatic()) {
            // for non-static methods, we know the this label
            // must be bounded above by the start label
            A.addAssertionLE(ct.thisLabel(), callerPcLabel);
        }

        A.setPc(callerPcLabel, lc);
        Label initialPCBound = A.currentCodePCBound();

        if (!endorseTo.isTop()) {
            // check that there is sufficient authority to endorse to 
            // the label endorseTo.
            JifEndorseExprExt.checkOneDimen(lc, A, Li, endorseTo, mi.position(),
                    false, true);
            JifEndorseExprExt.checkAuth(lc, A, Li, endorseTo, mi.position(),
                    false, true);

            // the initial pc bound is the endorseTo label
            initialPCBound = endorseTo;

            // add a restriction on the "callerPC" label. It is less
            // than the endorseTo label
            A.addAssertionLE(callerPcLabel, endorseTo);
        }

        A.setCurrentCodePCBound(initialPCBound);
        return initialPCBound;

    }

    /**
     * This method corresponds to the constraint-authority predicate in the
     * thesis (Figure 4.39).  It returns the set of principals for which the
     * method can act.
     */
    protected Set<Principal> constrainAuth(JifProcedureInstance mi,
            JifContext A) {
        Set<Principal> newAuth = new LinkedHashSet<Principal>();

        for (Assertion c : mi.constraints()) {
            if (c instanceof AuthConstraint) {
                AuthConstraint ac = (AuthConstraint) c;
                newAuth.addAll(ac.principals());
            }
        }

        return newAuth;
    }

    /** Adds the caller's authorities into <code>auth</code> */
    protected static void addCallers(JifProcedureInstance mi,
            Set<Principal> auth) {

        for (Assertion c : mi.constraints()) {
            if (c instanceof CallerConstraint) {
                final CallerConstraint cc = (CallerConstraint) c;
                auth.addAll(cc.principals());
            }
        }
    }

    /**
     * Check that there is a p' in the old "auth" set such that p' actsFor p.
     */
    protected void checkActsForAuthority(final Principal p, final JifContext A,
            LabelChecker lc) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) A.typeSystem();
        Principal authority = ts.conjunctivePrincipal(null, A.authority());

        String codeName = A.currentCode().toString();
        if (A.currentCode() instanceof JifProcedureInstance) {
            codeName = ((JifProcedureInstance) A.currentCode()).debugString();
        }

        final String msgCodeName = codeName;

        lc.constrain(authority, PrincipalConstraint.ACTSFOR, p, A.labelEnv(),
                A.currentCode().position(), new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "The authority of the class "
                                + A.currentClass().name()
                                + " is insufficient to act for principal " + p
                                + ".";
                    }

                    @Override
                    public String detailMsg() {
                        return "The " + msgCodeName
                                + " states that it has the authority of the "
                                + "principal " + p
                                + ". However, the conjunction of the authority"
                                + " set of the class is insufficient to act for "
                                + p + ".";
                    }
                });
    }

    /**
     * This method corresponds to the constraint-ph predicate in the thesis
     * (Figure 4.39).  It returns the principal hierarchy used to check the
     * body of the method.
     */
    public static void constrainLabelEnv(JifProcedureInstance mi, JifContext A,
            CallHelper ch) throws SemanticException {
        for (Assertion c : mi.constraints()) {
            if (c instanceof ActsForConstraint) {
                @SuppressWarnings("unchecked")
                ActsForConstraint<ActsForParam, ActsForParam> afc =
                        (ActsForConstraint<ActsForParam, ActsForParam>) c;
                ActsForParam actor = afc.actor();
                ActsForParam granter = afc.granter();
                if (ch != null) {
                    actor = ch.instantiate(A, actor);
                    granter = ch.instantiate(A, granter);
                }

                if (actor instanceof Principal
                        && granter instanceof Principal) {
                    Principal pActor = (Principal) actor;
                    Principal pGranter = (Principal) granter;
                    if (afc.isEquiv()) {
                        A.addEquiv(pActor, pGranter);
                    } else {
                        A.addActsFor(pActor, pGranter);
                    }
                } else if (actor instanceof Label
                        && granter instanceof Principal) {
                    A.addActsFor((Label) actor, (Principal) granter);
                } else {
                    throw new InternalCompilerError(afc.position(),
                            "Unexpected ActsForConstraint (" + actor.getClass()
                                    + " actsfor " + granter.getClass() + ").");
                }
            }
            if (c instanceof LabelLeAssertion) {
                LabelLeAssertion lla = (LabelLeAssertion) c;
                Label lhs = lla.lhs();
                Label rhs = lla.rhs();
                if (ch != null) {
                    lhs = ch.instantiate(A, lhs);
                    rhs = ch.instantiate(A, rhs);
                }
                A.addAssertionLE(lhs, rhs);
            }
        }
    }

    /**
     * This method corresponds to most of the check-body predicate in the
     * thesis (Figure 4.40).  It assumes the body has already been checked
     * and that the path map X is the join of the body's path map and the
     * initial path map of the method.
     *
     * It adds the constraints that associate return termination and
     * return value labels in the path map X with the declared return
     * label and associates the exception labels in the path map X with
     * the declared labels in the methods "throws" clause.
     */
    protected void addReturnConstraints(Label Li, PathMap X,
            JifProcedureInstance mi, LabelChecker lc, final Type returnType)
                    throws SemanticException {
        if (Report.should_report(jif_verbose, 2))
            Report.report(2, "Adding constraints for result of " + mi);

        ProcedureDecl mn = (ProcedureDecl) node();
        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();

        // Add the return termination constraints.

        // fold the call site pc into the return label
        Label Lr = lc.upperBound(mi.returnLabel(), ts.callSitePCLabel(mi));

        //Hack: If no other paths, the procedure must return. Therefore,
        //X.n is not taken, and X.r doesn't contain any information. 
        //TODO: implement a more precise single-path rule.
        //XXX: Somewhat experimental commenting of this check.
        // Why was is only considering methods that exited normally without
        // return statements?
        if (X.singlePath()) {
            X = X.N(ts.notTaken());
            X = X.R(ts.bottomLabel());
        } else {
            lc.constrain(
                    new NamedLabel("X.n",
                            "information that may be gained by the body terminating normally",
                            X.N()).join(lc, "X.r",
                                    "information that may be gained by exiting the body with a return statement",
                                    X.R()).join(lc, "Li",
                                            "Lower bound for method output",
                                            A.currentCodePCBound()),
                    LabelConstraint.LEQ,
                    new NamedLabel("Lr", "return label of the method", Lr),
                    A.labelEnv(), mn.position(), new ConstraintMessage() {
                        @Override
                        public String msg() {
                            return "The non-exception termination of the "
                                    + "method body may reveal more information "
                                    + "than is declared by the method return label.";
                        }

                        @Override
                        public String detailMsg() {
                            return "The method return label, " + namedRhs()
                                    + ", is an upper bound on how much "
                                    + "information can be gained by observing "
                                    + "that this method terminates normally "
                                    + "(i.e., terminates without throwing "
                                    + "an exception). The method body may "
                                    + "reveal more information than this. The "
                                    + "return label of a method is declared "
                                    + "after the variables, e.g. "
                                    + "\"void m(int i):{" + namedRhs() + "}\".";
                        }

                        @Override
                        public String technicalMsg() {
                            return "the return (end) label is less restricted than "
                                    + namedLhs() + " of the body.";
                        }
                    });
        }
        // return value constraints are implemented at the "return" statement, in order
        // to make use of the (more precise) label environment there.

        // Add the exception path constraints.
        for (Path path : X.paths()) {
            if (!(path instanceof ExceptionPath)) {
                continue;
            }

            ExceptionPath ep = (ExceptionPath) path;

            Label pathLabel = X.get(ep);

            if (pathLabel instanceof NotTaken) throw new InternalCompilerError(
                    "An exception path cannot be not taken");

            Type pathType = ep.exception();
            NamedLabel pathNamedLabel =
                    new NamedLabel("exc_" + pathType.toClass().name(),
                            "upper bound on information that may be gained "
                                    + "by observing the method throwing the exception "
                                    + pathType.toClass().name(),
                            pathLabel);

            List<? extends Type> throwTypes = mi.throwTypes();
            for (final Type tj : throwTypes) {
                Label Lj = ts.labelOfType(tj, Lr);

                // fold the call site pc into the return label
                Lj = lc.upperBound(Lj, ts.callSitePCLabel(mi));

                if (ts.isSubtype(pathType, tj) || ts.isSubtype(tj, pathType)) {
                    if (ts.isSubtype(pathType, tj)) {
                        SubtypeChecker subtypeChecker =
                                new SubtypeChecker(tj, pathType);
                        subtypeChecker.addSubtypeConstraints(lc, mn.position());
                    } else {

                        SubtypeChecker subtypeChecker =
                                new SubtypeChecker(pathType, tj);
                        subtypeChecker.addSubtypeConstraints(lc, mn.position());
                    }
                    if (Report.should_report(jif_verbose, 4)) Report.report(4,
                            ">>> X[C'] <= Lj (for exception " + tj + ")");

                    lc.constrain(pathNamedLabel, LabelConstraint.LEQ,
                            new NamedLabel("decl_exc_" + tj.toClass().name(),
                                    "declared upper bound on information that may be "
                                            + "gained by observing the method throwing the exception "
                                            + tj.toClass().name(),
                                    Lj),
                            A.labelEnv(), mi.position(),
                            new ConstraintMessage() {
                                @Override
                                public String msg() {
                                    return "More information may be gained "
                                            + "by observing a "
                                            + tj.toClass().fullName()
                                            + " exception than is permitted by the "
                                            + "method/constructor signature";
                                }

                                @Override
                                public String technicalMsg() {
                                    return "the path of <" + tj
                                            + "> may leak information "
                                            + "more restrictive than the join of the declared "
                                            + "exception label and the return(end) label";
                                }
                            });
                }
            }
        }
    }

    /**
     * Check that covariant labels do not appear in contravariant positions
     * @param mi
     * @param lc
     * @throws SemanticException 
     */
    protected void checkConstraintVariance(JifProcedureInstance mi,
            LabelChecker lc) throws SemanticException {
        if (!(lc.context().currentCode() instanceof ConstructorInstance)) {
            for (Assertion c : mi.constraints()) {
                if (c instanceof LabelLeAssertion) {
                    LabelLeAssertion lle = (LabelLeAssertion) c;
                    lle.rhs().subst(
                            new ConstraintVarianceLabelChecker(c.position()));
                }
            }
        }
    }

    /**
     * Checker to ensure that labels do not use
     * covariant labels in the wrong places
     */
    protected static class ConstraintVarianceLabelChecker
            extends LabelSubstitution {
        private Position declPosition;

        ConstraintVarianceLabelChecker(Position declPosition) {
            this.declPosition = declPosition;
        }

        @Override
        public Label substLabel(Label L) throws SemanticException {
            if (L.isCovariant()) {
                throw new SemanticDetailedException(
                        "Covariant labels cannot occur on the right hand side of label constraints.",
                        "The right hand side of a label constraint cannot contain the covariant components such as "
                                + L + ". ",
                        declPosition);
            }
            return L;
        }
    }

}
