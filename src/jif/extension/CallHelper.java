package jif.extension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jif.JifOptions;
import jif.ast.JifExt_c;
import jif.ast.JifInstantiator;
import jif.ast.JifNodeFactory;
import jif.types.ActsForConstraint;
import jif.types.ActsForParam;
import jif.types.Assertion;
import jif.types.AuthConstraint;
import jif.types.AutoEndorseConstraint;
import jif.types.CallerConstraint;
import jif.types.ConstraintMessage;
import jif.types.JifClassType;
import jif.types.JifContext;
import jif.types.JifMethodInstance;
import jif.types.JifProcedureInstance;
import jif.types.JifTypeSystem;
import jif.types.LabelConstraint;
import jif.types.LabelLeAssertion;
import jif.types.LabelSubstitution;
import jif.types.LabeledType;
import jif.types.NamedLabel;
import jif.types.PathMap;
import jif.types.PrincipalConstraint;
import jif.types.SemanticDetailedException;
import jif.types.label.AccessPath;
import jif.types.label.AccessPathLocal;
import jif.types.label.AccessPathRoot;
import jif.types.label.ArgLabel;
import jif.types.label.Label;
import jif.types.label.VarLabel;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;

import polyglot.ast.Expr;
import polyglot.ast.Local;
import polyglot.ast.ProcedureCall;
import polyglot.ast.Receiver;
import polyglot.main.Report;
import polyglot.types.ConstructorInstance;
import polyglot.types.LocalInstance;
import polyglot.types.MethodInstance;
import polyglot.types.ProcedureInstance;
import polyglot.types.ReferenceType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.StringUtil;

/**
 * This is a tool to label-check method calls. This class should be used by
 * creating an instance of it, and then calling the method
 * {@link #checkCall(LabelChecker, List, boolean) checkCall(LabelChecker)}. After the
 * call to that method, the remaining methods (which are getter methods) may be
 * called.
 */
public class CallHelper {
    protected static boolean shouldReport(int obscurity) {
        return Report.should_report(jif.Topics.labels, obscurity);
    }

    protected static void report(int obscurity, String s) {
        Report.report(obscurity, "labels: " + s);
    }

    /**
     * Label of the reference to the object on which the procedure is being
     * called.
     */
    protected final Label receiverLabel;

    protected final Expr receiverExpr;

    protected final ReferenceType calleeContainer;

    /**
     * Copy of the list of the <code>Expr</code> s that are the arguments to
     * the procedure call. As we label check the argument expressions, we
     * replace the elements of this list with the label checked versions of the
     * argument expressions.
     */
    protected final List<Expr> actualArgs;

    /**
     * The procedure being called. Also the MethodInstance of the overridden
     * method, when this class is used for overriding checking.
     */
    protected final JifProcedureInstance pi;

    /**
     * The position of the procedure call
     */
    protected final Position position;

    /**
     * Labels of the actual arguments.
     */
    protected List<Label> actualArgLabels;

    /**
     * Labels of the actual parameters.
     */
    protected List<Label> actualParamLabels;

    /**
     * The PathMap for the procedure call.
     */
    protected PathMap X;

    /**
     * The return type of the procedure, if there is one.
     */
    protected Type returnType;

    /*
     * Flags to ensure that this class is used correctly.
     */
    protected boolean callChecked = false;
    protected final boolean overrideChecker; // false if call checker, true if override checker

    /**
     * Method instance of the overriding (subclasses') method. Used
     * only for override checking.
     */
    protected JifMethodInstance overridingMethod = null;

    public CallHelper(Label receiverLabel, Receiver receiver,
            ReferenceType calleeContainer, JifProcedureInstance pi,
            List<Expr> actualArgs, Position position) {
        this(receiverLabel, receiver, calleeContainer, pi, actualArgs, position,
                false);
    }

    protected CallHelper(Label receiverLabel, Receiver receiver,
            ReferenceType calleeContainer, JifProcedureInstance pi,
            List<Expr> actualArgs, Position position, boolean overrideChecker) {
        this.receiverLabel = receiverLabel;
        this.calleeContainer = calleeContainer;
        this.overrideChecker = overrideChecker;
        if (receiver instanceof Expr) {
            this.receiverExpr = (Expr) receiver;
        } else {
            this.receiverExpr = null;
        }
        this.actualArgs = new ArrayList<Expr>(actualArgs);
        this.pi = pi;
        this.position = position;
        this.callChecked = false;

        if (pi.formalTypes().size() != actualArgs.size())
            throw new InternalCompilerError("Wrong number of args.");
    }

    public static CallHelper OverrideHelper(JifMethodInstance overridden,
            JifMethodInstance overriding, LabelChecker lc) {

        JifTypeSystem jts = (JifTypeSystem) overridden.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) lc.nodeFactory();
        JifClassType subContainer = (JifClassType) overriding.container();
        Label receiverLabel = subContainer.thisLabel();
        Receiver receiver = nf.This(overriding.position());
        ReferenceType calleeContainer = overridden.container().toReference();

        List<Expr> actualArgs =
                new ArrayList<Expr>(overriding.formalTypes().size());

        for (Type t : overriding.formalTypes()) {
            if (jts.isLabeled(t)) {
                ArgLabel al = (ArgLabel) jts.labelOfType(t);
                LocalInstance formalInst = (LocalInstance) al.formalInstance();
                Local l = nf
                        .Local(formalInst.position(),
                                nf.Id(al.position(), al.name()))
                        .localInstance(formalInst);
                actualArgs.add(l);
            } else {
                throw new InternalCompilerError("Formal type is not labeled!");
            }
        }

        CallHelper ch = new CallHelper(receiverLabel, receiver, calleeContainer,
                overridden, actualArgs, overriding.position(), true);
        ch.overridingMethod = overriding;
        ch.actualParamLabels = Collections.emptyList();
        ch.actualArgLabels =
                new ArrayList<Label>(overriding.formalTypes().size());

        for (Type t : overriding.formalTypes()) {
            ArgLabel al = (ArgLabel) jts.labelOfType(t);
            ch.actualArgLabels.add(al);
        }

        return ch;
    }

    public Type returnType() {
        if (overrideChecker) {
            throw new InternalCompilerError("Not available for call checking");
        }
        if (!callChecked) {
            throw new InternalCompilerError("checkCall not yet called!");
        }
        return returnType;
    }

    public List<Expr> labelCheckedArgs() {
        if (overrideChecker) {
            throw new InternalCompilerError("Not available for call checking");
        }
        if (!callChecked) {
            throw new InternalCompilerError("checkCall not yet called!");
        }
        return actualArgs;
    }

    public PathMap X() {
        if (overrideChecker) {
            throw new InternalCompilerError("Not available for call checking");
        }
        if (!callChecked) {
            throw new InternalCompilerError("checkCall not yet called!");
        }
        return X;
    }

    protected PathMap labelCheckAndConstrainParams(LabelChecker lc,
            List<Type> throwTypes) throws SemanticException {
        PathMap Xjoin;
        JifTypeSystem ts = lc.typeSystem();
        LabelTypeCheckUtil ltcu = ts.labelTypeCheckUtil();

        // If it's a constructor or a static method call, label check
        // the class type, since that may reveal some information.
        // if the method call is a constructor call, or a static method call,
        // then we need to check the pathmap.
        if (this.pi.flags().isStatic()) {
            Xjoin = ltcu.labelCheckType(pi.container(), lc, throwTypes,
                    position);
            List<PathMap> Xparams = ltcu.labelCheckTypeParams(pi.container(),
                    lc, throwTypes, position);
            actualParamLabels = new ArrayList<Label>(Xparams.size());
            for (PathMap Xj : Xparams) {
                actualParamLabels.add(Xj.NV());
            }
        } else if (this.pi instanceof ConstructorInstance) {
            Xjoin = ltcu.labelCheckType(pi.container(), lc, throwTypes,
                    position);
            // now constraint params, pretending that they will be args to the constructor with upper bound {this}.
            List<PathMap> Xparams = ltcu.labelCheckTypeParams(pi.container(),
                    lc, throwTypes, position);
            actualParamLabels = new ArrayList<Label>(Xparams.size());
            JifContext A = lc.context();

            NamedLabel paramUB = new NamedLabel("param_upper_bound",
                    "the upper bound on the information that may be revealed by any actual parameter",
                    this.receiverLabel);

            int counter = 0;
            for (PathMap Xj : Xparams) {
                actualParamLabels.add(Xj.NV());
                final int count = ++counter;
                lc.constrain(
                        new NamedLabel("actual_param_" + count,
                                "the label of the " + StringUtil.nth(count)
                                        + " actual parameter",
                                Xj.NV()),
                        LabelConstraint.LEQ, paramUB, A.labelEnv(),
                        this.position, new ConstraintMessage() {
                            @Override
                            public String msg() {
                                return "The actual parameter is more restrictive than "
                                        + "permitted.";
                            }
                        });
            }
            if (lc.context().inConstructorCall()) {
                // the evaluation of the parameters in a constructor call
                // super(...) or this(...) type doesn't reveal any info, since
                // they are either constants or formal params. As such, in the
                // same way we can treat the initial pc of a constructor as
                // the bottom label, we can ignore the "this" label that is
                // introduced by label checking formal params.
                Xjoin = Xjoin.N(lc.context().pc());
            }
        } else {
            Xjoin = ts.pathMap().N(lc.context().pc());
            actualParamLabels = Collections.emptyList();
        }

        return Xjoin;
    }

    /**
     * Label check the arguments. Also initializes the array
     * <code>argLabels</code>, that is the labels of the actual arguments.
     * 
     * @param Xjoin The pathmap for the call so far, that is, up to the point
     *            just before the evaluation of the actual arguments.
     * @return The PathMap for the evaluation of all actual arguments, that is,
     *         join_j xj, for all actual arguments xj.
     */
    protected PathMap labelCheckAndConstrainArgs(LabelChecker lc, PathMap Xjoin)
            throws SemanticException {
        JifContext A = lc.context();
        JifTypeSystem ts = lc.typeSystem();

        // X_0 = X_null[n := A[pc]]
        PathMap Xj = ts.pathMap();
        Xj = Xj.N(A.pc());

        this.actualArgLabels = new ArrayList<Label>(this.actualArgs.size());

        for (int i = 0; i < actualArgs.size(); i++) {
            Expr Ej = actualArgs.get(i);

            // A[pc := X_{j-1}[N]] |- Ej : Xj
            A = (JifContext) A.pushBlock();
            updateForNextArg(lc, A, Xj);
            Ej = (Expr) lc.context(A).labelCheck(Ej);
            A = (JifContext) A.pop();

            actualArgs.set(i, Ej);

            Xj = JifExt_c.getPathMap(Ej);
            actualArgLabels.add(Xj.NV());
            Xjoin = Xjoin.join(Xj);
        }

        // now that the actualArgs list is correct, we can constrain the args
        for (int i = 0; i < actualArgs.size(); i++) {
            Expr Ej = actualArgs.get(i);
            constrainArg(lc, i, Ej, pi.formalTypes().get(i));
        }

        return Xjoin;
    }

    /**
     * Utility method to abstract away passing along label information in the
     * context between arguments during checking.
     */
    protected void updateForNextArg(LabelChecker lc, JifContext A,
        PathMap Xprev) {
        A.setPc(Xprev.N(), lc);
    }

    /**
     * Add constraints to ensure that the labels of the actual arguments
     * are less than the upper bounds of the formal arguments.
     * @param index the ith arg
     * @param Ej the expression of the ith actual arg
     * @param formalType the type of ith formal arg.
     * 
     */
    protected void constrainArg(LabelChecker lc, final int index, final Expr Ej,
            Type formalType) throws SemanticException {
        // constrain the labels of the argument to be less
        // than the bound of the formal arg, substituting in the
        // fresh labels we just created.
        JifContext A = lc.context();
        JifTypeSystem jts = (JifTypeSystem) A.typeSystem();

        ArgLabel aj = (ArgLabel) jts.labelOfType(formalType);

        // the upper bound label of the formal argument
        Label argBoundj = instantiate(A, aj.upperBound());

        // A |- Xj[nv] <= argLj
        PathMap Xj = JifExt_c.getPathMap(Ej);
        lc.constrain(
                new NamedLabel("actual_arg_" + (index + 1),
                        "the label of the " + StringUtil.nth(index + 1)
                                + " actual argument",
                        Xj.NV()),
                LabelConstraint.LEQ,
                new NamedLabel("formal_arg_" + (index + 1),
                        "the upper bound of the formal argument "
                                + aj.formalInstance().name(),
                        argBoundj),
                A.labelEnv(), Ej.position(), new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "The actual argument is more restrictive than "
                                + "the formal argument.";
                    }

                    @Override
                    public String detailMsg() {
                        return "The label of the actual argument, " + namedLhs()
                                + ", is more restrictive than the label of the "
                                + "formal argument, " + namedRhs() + ".";
                    }

                    @Override
                    public String technicalMsg() {
                        return "Invalid argument: the actual argument <" + Ej
                                + "> has a more restrictive label than the "
                                + "formal argument.";
                    }
                });

        // Must check that the actual is a subtype of the formal.
        // Most of this is done in typeCheck, but if actual and formal
        // are instantitation types, we must add constraints for the
        // labels.
        SubtypeChecker sc =
                new SubtypeChecker(instantiate(A, jts.unlabel(formalType)),
                        jts.unlabel(Ej.type()));
        sc.addSubtypeConstraints(lc, Ej.position());
    }

    /**
     * Make sure that the actual arg for
     * any formal arg that appears in the signature is final.
     * @throws SemanticException
     *
     */
    protected void constrainFinalActualArgs(JifTypeSystem jts)
            throws SemanticException {
        // find which formal arguments appear in the signature
        final Set<LocalInstance> argInstances =
                new LinkedHashSet<LocalInstance>();
        LabelSubstitution argLabelGather = new LabelSubstitution() {
            @Override
            public AccessPath substAccessPath(AccessPath ap) {
                extractRoot(ap.root());
                return ap;
            }

            void extractRoot(AccessPathRoot root) {
                if (root instanceof AccessPathLocal) {
                    argInstances.add(((AccessPathLocal) root).localInstance());
                }
            }
        };

        pi.subst(argLabelGather);

        // now go through each of the actual and formal arguments, and
        // check if the actual arg needs to be final.
        Iterator<? extends Type> formalTypes = pi.formalTypes().iterator();
        for (int j = 0; j < actualArgs.size(); j++) {
            Type tj = formalTypes.next();
            ArgLabel aj = (ArgLabel) jts.labelOfType(tj);
            if (argInstances.contains(aj.formalInstance())) {
                // this actual arg needs to be final!
                Expr Ej = actualArgs.get(j);
                if (!jts.isFinalAccessExprOrConst(Ej)) {
                    throw new SemanticDetailedException(
                            "The " + StringUtil.nth(j + 1)
                                    + " argument must be a final access path or a "
                                    + "constant",
                            "The " + StringUtil.nth(j + 1) + " formal argument "
                                    + "of " + pi.debugString()
                                    + " is used to express a dynamic label or principal "
                                    + "in the procedure's signature. As such, "
                                    + "the actual argument must be a final "
                                    + "access path or a constant.",
                            Ej.position());
                }
            }

        }
    }

    protected Label resolvePCBound(LabelChecker lc) throws SemanticException {
        return instantiate(lc.context(), pi.pcBound());
    }

    /**
     * Returns the instantiated return label.
     * @throws SemanticException
     */
    protected Label resolveReturnLabel(LabelChecker lc)
            throws SemanticException {
        return instantiate(lc.context(), pi.returnLabel());
    }

    /**
     * Returns the instantiated return value label joined with returnLabel.
     * @throws SemanticException
     */
    protected Label resolveReturnValueLabel(LabelChecker lc, Label returnLabel)
            throws SemanticException {
        JifTypeSystem ts = lc.typeSystem();
        Label L = null;

        if (pi instanceof MethodInstance) {
            MethodInstance mi = (MethodInstance) pi;
            L = instantiate(lc.context(), ts.labelOfType(mi.returnType()));
        } else {
            L = ts.bottomLabel(pi.position());
        }

        return lc.upperBound(L, returnLabel);
    }

    protected PathMap excPathMap(LabelChecker lc, Label returnLabel,
            Label pcPriorToInvoke, List<Type> throwTypes)
                    throws SemanticException {
        JifTypeSystem ts = lc.typeSystem();
        PathMap Xexn = ts.pathMap();

        for (Type te : pi.throwTypes()) {
            Label Le = ts.labelOfType(te, returnLabel);
            Le = instantiate(lc.context(), Le);
            JifExt_c.checkAndRemoveThrowType(throwTypes, te);
            Xexn = Xexn.exception(te, lc.upperBound(Le, pcPriorToInvoke));
        }

        return Xexn;
    }

    /**
     * Check method calls. (Thesis, Figure 4.29)
     * 
     * 
     */
    public <N extends ProcedureCall> N checkCall(LabelChecker lc,
        List<Type> throwTypes, N call, boolean targetMayBeNull) throws
      SemanticException {

        if (overrideChecker) {
            throw new InternalCompilerError("Not available for call checking");
        }
        if (callChecked) {
            throw new InternalCompilerError("checkCall already called!");
        }

        JifTypeSystem ts = lc.typeSystem();

        // A |- call-begin(ct, args, mti)
        if (shouldReport(4)) report(4, ">>>>> call-begin");

        // check parameters
        PathMap Xjoin = labelCheckAndConstrainParams(lc, throwTypes);
        lc = lc.context((JifContext) lc.context().pushBlock());
        lc.context().setPc(Xjoin.N(), lc);

        // check arguments
        Xjoin = labelCheckAndConstrainArgs(lc, Xjoin);
        if (targetMayBeNull) {
            // a null pointer exception may be thrown
            JifExt_c.checkAndRemoveThrowType(throwTypes,
                    ts.NullPointerException());
            Xjoin = Xjoin.exc(receiverLabel, ts.NullPointerException());
        }
        constrainFinalActualArgs(ts);
        lc = lc.context((JifContext) lc.context().pushBlock());
        lc.context().setPc(Xjoin.N(), lc);

        // A |- X_{maxj}[N] + entry_pc <= Li
        Label Li = resolvePCBound(lc);
        if (Li != null) {
            final ProcedureInstance callee = pi;
            JifContext A = lc.context();

            // the path map for the last argument.
            NamedLabel namedLi = new NamedLabel("callee_PC_bound",
                    "lower bound on the side effects of the method "
                            + callee.signature(),
                    Li);

            NamedLabel namedPc = new NamedLabel("pc",
                    "label of the program counter at this call site",
                    ts.join(Xjoin.N(), A.currentCodePCBound()));

            lc.constrain(namedPc, LabelConstraint.LEQ, namedLi, A.labelEnv(),
                    position, new ConstraintMessage() {
                        @Override
                        public String msg() {
                            return "PC at call site more restrictive than "
                                    + "begin label of " + callee.signature()
                                    + ".";
                        }

                        @Override
                        public String detailMsg() {
                            return "Calling the method at this program point may "
                                    + "reveal too much information to the receiver of "
                                    + "the method call. " + callee.signature()
                                    + " can only be invoked "
                                    + "if the invocation will reveal no more "
                                    + "information than the callee's begin label, "
                                    + namedRhs()
                                    + ". However, execution reaching "
                                    + "this program point may depend on information "
                                    + "up to the PC at this program point: "
                                    + namedLhs() + ".";
                        }

                        @Override
                        public String technicalMsg() {
                            return "Invalid method call: " + namedLhs()
                                    + " is more restrictive than "
                                    + "the callee's begin label.";
                        }
                    });
        }

        satisfiesConstraints(pi, lc, true);

        // A |- call-end(ct, pi, Aa, Li, LrvDef)
        if (shouldReport(4)) report(4, ">>>>> call-end");

        Label Lr = resolveReturnLabel(lc);
        Label Lrv = resolveReturnValueLabel(lc, Lr);

        // pcPriorToInvoke is the pc label after the evaluation of the receiver and args,
        // but before the invocation of the method.
        Label pcPriorToInvoke = Xjoin.N();

        // normal termination label after invocation is the join of the return label and the pc
        X = Xjoin.N(lc.upperBound(Lr, pcPriorToInvoke));

        // normal value label after invocation is the join of the return value label and the pc
        X = X.NV(lc.upperBound(Lrv, pcPriorToInvoke));

        // exceptions that the method may throw.
        X = X.join(excPathMap(lc, Lr, pcPriorToInvoke, throwTypes));

        if (pi instanceof MethodInstance) {
            returnType = instantiate(lc.context(),
                    ((MethodInstance) pi).returnType());
        } else {
            returnType = null;
        }

        // record that this method has now been called.
        callChecked = true;

        return call;
    }

    /** Check if the caller has sufficient authority, and label constraints
     * are satisfied.
     *  Thesis, Figure 4.29.
     *  This method is called both when checking calls, and when checking overrides. When
     *  checking overrides, since we are passed the overriding method (as opposed to the
     *  overridden method) we do not perform instantiations of principals and labels from jpi.
     */
    protected void satisfiesConstraints(final JifProcedureInstance jpi,
            LabelChecker lc, boolean performInstantiations)
                    throws SemanticException {
        final JifContext A = lc.context();
        // Check method-level and class-level constraints
        List<Assertion> constraints =
                new LinkedList<Assertion>(jpi.constraints());
        if (jpi.container() instanceof JifClassType)
            constraints.addAll(((JifClassType) jpi.container()).constraints());

        for (Assertion jc : constraints) {

            if (jc instanceof AuthConstraint
                    || jc instanceof AutoEndorseConstraint) {
                continue;
            } else if (jc instanceof CallerConstraint) {
                CallerConstraint jcc = (CallerConstraint) jc;

                // construct a principal representing the authority of the context
                Principal authPrincipal = lc.jifTypeSystem()
                        .conjunctivePrincipal(jpi.position(), A.authority());
                final NamedLabel provider =
                        new NamedLabel(A.provider().toString(),
                                "provider of "
                                        + A.provider().classType().fullName(),
                        A.provider());
                JifOptions opt = (JifOptions) lc.jifTypeSystem().extensionInfo()
                        .getOptions();
                // Check the authority
                for (Principal orig : jcc.principals()) {
                    final Principal pi =
                            performInstantiations ? instantiate(A, orig) : orig;

                    ConstraintMessage cmsg = new ConstraintMessage() {
                        @Override
                        public String msg() {
                            if (!overrideChecker) {
                                return "The caller must have the authority of the principal "
                                        + pi + " to invoke "
                                        + jpi.debugString();
                            } else {
                                return "The subclass cannot assume the caller has the "
                                        + "authority of the principal " + pi;
                            }
                        }

                        @Override
                        public String detailMsg() {
                            if (!overrideChecker) {
                                return "The " + jpi.debugString()
                                        + " requires that the "
                                        + "caller of the method have the authority of "
                                        + "the principal " + pi
                                        + ". The caller does "
                                        + "not have this principal's authority.";
                            } else {
                                return "The " + jpi.debugString()
                                        + " requires that the "
                                        + "caller of the method have the authority of "
                                        + "the principal " + pi
                                        + ". However, this "
                                        + "method overrides the method in class "
                                        + CallHelper.this.pi.container()
                                        + " which does not make this requirement.";
                            }
                        }
                    };
                    // authPrincipal must actfor pi, i.e., at least one
                    // of the principals that have authorized the code must act for pi.
                    if (opt.authFromProvider()) {
                        lc.constrain(provider, pi, A.labelEnv(),
                                overrideChecker ? jcc.position() : position,
                                cmsg);
                    } else {
                        lc.constrain(authPrincipal, PrincipalConstraint.ACTSFOR,
                                pi, A.labelEnv(),
                                overrideChecker ? jcc.position() : position,
                                cmsg);
                    }
                }
            } else if (jc instanceof ActsForConstraint) {
                @SuppressWarnings("unchecked")
                final ActsForConstraint<ActsForParam, ActsForParam> jac =
                        (ActsForConstraint<ActsForParam, ActsForParam>) jc;
                ActsForParam actor = jac.actor();
                ActsForParam granter = jac.granter();
                if (actor instanceof Principal
                        && granter instanceof Principal) {
                    @SuppressWarnings("unchecked")
                    ActsForConstraint<Principal, Principal> jpapc =
                            (ActsForConstraint<Principal, Principal>) jc;
                    satisfiesPrincipalActsForPrincipalConstraint(jpi, jpapc, lc,
                            performInstantiations);
                } else if (actor instanceof Label
                        && granter instanceof Principal) {
                    satisfiesLabelActsForPrincipalConstraint(jpi,
                            jac.position(), (Label) actor, (Principal) granter,
                            lc, performInstantiations);
                } else if (actor instanceof Label && granter instanceof Label) {
                    // XXX IMPLEMENT ME
                    throw new InternalCompilerError(
                            "acts-for constraints between labels not implemented yet");
                } else {
                    throw new InternalCompilerError(jac.position(),
                            "Unexpected ActsForConstraint (" + actor.getClass()
                                    + " actsfor " + granter.getClass() + ").");
                }
            } else if (jc instanceof LabelLeAssertion) {
                LabelLeAssertion lla = (LabelLeAssertion) jc;
                satisfiesLabelLeAssertion(jpi, lla.position(), lla.lhs(),
                        lla.rhs(), lc, performInstantiations);
            }
        }
    }

    /**
     * Helper for satisfiesConstraints().
     */
    private void satisfiesPrincipalActsForPrincipalConstraint(
            final JifProcedureInstance jpi,
            final ActsForConstraint<Principal, Principal> constraint,
            LabelChecker lc, boolean performInstantiations)
                    throws SemanticException {
        final JifContext A = lc.jifContext();
        final Principal actor, granter;
        if (performInstantiations) {
            actor = instantiate(A, constraint.actor());
            granter = instantiate(A, constraint.granter());
        } else {
            actor = constraint.actor();
            granter = constraint.granter();
        }

        if (constraint.isEquiv()) {
            lc.constrain(actor, PrincipalConstraint.EQUIV, granter,
                    A.labelEnv(), position, new ConstraintMessage() {
                        @Override
                        public String msg() {
                            if (!overrideChecker) {
                                return "The principal " + actor
                                        + " must be equivalent to " + granter
                                        + " to invoke " + jpi.debugString();
                            } else {
                                return "The subclass cannot assume that "
                                        + actor + " is " + "equivalent to "
                                        + granter;

                            }
                        }

                        @Override
                        public String detailMsg() {
                            if (!overrideChecker) {
                                return "The " + jpi.debugString()
                                        + " requires that the "
                                        + " relationship " + constraint
                                        + " holds at the call site.";
                            } else {
                                return "The " + jpi.debugString()
                                        + " requires that " + actor + " is "
                                        + "equivalent to " + granter
                                        + ". However, this "
                                        + "method overrides the method in class "
                                        + pi.container()
                                        + " which does not make this requirement.";

                            }
                        }
                    });
        } else {
            lc.constrain(actor, PrincipalConstraint.ACTSFOR, granter,
                    A.labelEnv(), position, new ConstraintMessage() {
                        @Override
                        public String msg() {
                            if (!overrideChecker) {
                                return "The principal " + actor
                                        + " must act for " + granter
                                        + " to invoke " + jpi.debugString();
                            } else {
                                return "The subclass cannot assume that "
                                        + actor + " can " + "actfor " + granter;

                            }
                        }

                        @Override
                        public String detailMsg() {
                            if (!overrideChecker) {
                                return "The " + jpi.debugString()
                                        + " requires that the "
                                        + "relationship " + actor + " actsfor "
                                        + granter + " holds at the call site.";
                            } else {
                                return "The " + jpi.debugString()
                                        + " requires that " + actor + " can "
                                        + "actfor " + granter
                                        + ". However, this "
                                        + "method overrides the method in class "
                                        + pi.container()
                                        + " which does not make this requirement.";
                            }
                        }
                    });

        }
    }

    /**
     * Helper for satisfiesConstraints().
     */
    private void satisfiesLabelActsForPrincipalConstraint(
            final JifProcedureInstance jpi, Position pos, Label label,
            Principal principal, LabelChecker lc, boolean performInstantiations)
                    throws SemanticException {
        final JifContext A = lc.jifContext();
        final Label lhs;
        final Principal rhs;
        if (performInstantiations) {
            lhs = instantiate(A, label);
            rhs = instantiate(A, principal);
        } else {
            lhs = label;
            rhs = principal;
        }

        if (!overrideChecker) {
            // being used as a normal call checker
            pos = position;
        } else {
            // being used as an override checker
        }

        lc.constrain(new NamedLabel(label.toString(),
                "LHS of actsfor assertion", lhs), rhs, A.labelEnv(), pos,
                new ConstraintMessage() {
                    @Override
                    public String msg() {
                        if (!overrideChecker) {
                            // being used as a normal call checker
                            return "The label " + lhs + " must act for " + rhs
                                    + " to invoke " + jpi.debugString();
                        } else {
                            // being used as an override checker
                            return "The subclass cannot assume that " + lhs
                                    + " <= " + rhs;
                        }
                    }

                    @Override
                    public String detailMsg() {
                        if (!overrideChecker) {
                            // being used as a normal call checker
                            return "The " + jpi.debugString()
                                    + " requires that the " + "relationship "
                                    + lhs + " actsfor " + rhs
                                    + " holds at the call site.";
                        } else {
                            // being used as an override checker
                            return "The " + jpi.debugString()
                                    + " requires that " + lhs + " actsfor "
                                    + rhs + ". However, this "
                                    + "method overrides the method in class "
                                    + CallHelper.this.pi.container()
                                    + " which does not make this requirement.";
                        }
                    }
                });
    }

    /**
     * Helper for satisfiesConstraints().
     */
    private void satisfiesLabelLeAssertion(final JifProcedureInstance jpi,
            Position pos, Label left, Label right, LabelChecker lc,
            boolean performInstantiations) throws SemanticException {
        final JifContext A = lc.jifContext();
        final Label lhs, rhs;
        if (performInstantiations) {
            lhs = instantiate(A, left);
            rhs = instantiate(A, right);
        } else {
            lhs = left;
            rhs = right;
        }

        if (!overrideChecker) {
            // being used as a normal call checker
            pos = position;
        } else {
            // being used as an override checker
        }

        lc.constrain(
                new NamedLabel(left.toString(), "LHS of label assertion", lhs),
                LabelConstraint.LEQ,
                new NamedLabel(right.toString(), "RHS of label assertion", rhs),
                A.labelEnv(), pos, new ConstraintMessage() {
                    @Override
                    public String msg() {
                        if (!overrideChecker) {
                            // being used as a normal call checker
                            return "The label " + lhs
                                    + " must be less restrictive than " + rhs
                                    + " to invoke " + jpi.debugString();
                        } else {
                            // being used as an override checker
                            return "The subclass cannot assume that " + lhs
                                    + " <= " + rhs;
                        }
                    }

                    @Override
                    public String detailMsg() {
                        if (!overrideChecker) {
                            // being used as a normal call checker
                            return "The " + jpi.debugString()
                                    + " requires that the " + "relationship "
                                    + lhs + " <= " + rhs
                                    + " holds at the call site.";
                        } else {
                            // being used as an override checker
                            return "The " + jpi.debugString()
                                    + " requires that " + lhs + " <= " + rhs
                                    + ". However, this "
                                    + "method overrides the method in class "
                                    + CallHelper.this.pi.container()
                                    + " which does not make this requirement.";
                        }
                    }
                });
    }

    /**
     * Bind the given var labels to the appropriate labels that have been
     * calculated during the call checking.
     * 
     * @param lc the LabelChecker
     * @param receiverVarLabel the VarLabel used to stand in for the label of
     *            the receiver
     * @param actualArgVarLabels a list of VarLabels that were used to stand in
     *            for the labels of the actual arguments
     * @param actualParamVarLabels a list of VarLabels that were used to stand
     *            in for the labels of the actual parameters
     * @throws SemanticException
     */
    public void bindVarLabels(LabelChecker lc, VarLabel receiverVarLabel,
            List<? extends Label> actualArgVarLabels,
            List<? extends Label> actualParamVarLabels)
                    throws SemanticException {
        if (overrideChecker) {
            throw new InternalCompilerError("Not available for call checking");
        }
        if (!callChecked) {
            throw new InternalCompilerError("checkCall not yet called!");
        }
        JifContext A = lc.context();

        // bind the receiver var label
        if (receiverVarLabel != null && this.receiverLabel != null) {
            lc.constrain(
                    new NamedLabel(receiverVarLabel.componentString(),
                            receiverVarLabel),
                    LabelConstraint.EQUAL,
                    new NamedLabel(receiverVarLabel.componentString(),
                            this.receiverLabel),
                    A.labelEnv(), this.position);
        } else if (receiverVarLabel != null || this.receiverLabel != null) {
            throw new InternalCompilerError("Inconsistent receiver labels",
                    position);
        }

        // bind all the actual arg var labels
        for (int i = 0; i < actualArgLabels.size(); i++) {
            VarLabel argVarLbl = (VarLabel) actualArgVarLabels.get(i);
            Label argLbl = this.actualArgLabels.get(i);
            lc.constrain(new NamedLabel(argVarLbl.componentString(), argVarLbl),
                    LabelConstraint.EQUAL,
                    new NamedLabel(argVarLbl.componentString(), argLbl),
                    A.labelEnv(), this.position);
        }

        // bind all the actual param var labels.
        // that is, the labels of the parameter values in the type are bound
        // to the variables that represent them.
        // e.g., in the constructor call "new C[lbl1, o.lbl2]()", the values
        // of the expression "lbl1" and "o.lbl2" may reveal information, and
        // thus the labels of the expressions need to be considered. This only
        // needs to be done for static methods and constructor calls.

        if (this.pi.flags().isStatic()
                || this.pi instanceof ConstructorInstance) {
            for (int i = 0; i < actualParamVarLabels.size(); i++) {
                VarLabel paramVarLbl = (VarLabel) actualParamVarLabels.get(i);
                Label paramLbl = this.actualParamLabels.get(i);
                lc.constrain(
                        new NamedLabel(paramVarLbl.componentString(),
                                paramVarLbl),
                        LabelConstraint.EQUAL,
                        new NamedLabel(paramVarLbl.componentString(), paramLbl),
                        A.labelEnv(), this.position);
            }
        }
    }

    protected static List<ArgLabel> getArgLabelsFromFormalTypes(
            List<? extends Type> formalTypes, JifTypeSystem jts, Position pos)
                    throws SemanticException {
        List<ArgLabel> formalArgLabels =
                new ArrayList<ArgLabel>(formalTypes.size());
        for (Type t : formalTypes) {
            Label l = jts.labelOfType(t);
            if (!(l instanceof ArgLabel)) {
                throw new SemanticException(
                        "Internal label error, probably caused by an earlier error.",
                        pos);
            }
            ArgLabel al = (ArgLabel) l;
            formalArgLabels.add(al);
        }
        return formalArgLabels;
    }

    public Label instantiate(JifContext A, Label L) throws SemanticException {
        return JifInstantiator.instantiate(L, A, receiverExpr, calleeContainer,
                receiverLabel,
                getArgLabelsFromFormalTypes(pi.formalTypes(),
                        (JifTypeSystem) pi.typeSystem(), pi.position()),
                pi.formalTypes(), this.actualArgLabels, this.actualArgs,
                this.actualParamLabels);
    }

    public Set<Principal> instantiate(JifContext A, Set<Principal> s)
            throws SemanticException {
        Set<Principal> newS = new LinkedHashSet<Principal>();
        for (Principal p : s) {
            newS.add(instantiate(A, p));
        }
        return newS;
    }

    /**
     * replaces any signature ArgLabels in p with the appropriate label, and
     * replaces any signature ArgPrincipal with the appropriate prinicipal.
     * @throws SemanticException
     */
    public <P extends ActsForParam> P instantiate(JifContext A, P param)
            throws SemanticException {
        if (param instanceof Principal) {
            @SuppressWarnings("unchecked")
            P p = (P) instantiate(A, (Principal) param);
            return p;
        }

        if (param instanceof Label) {
            @SuppressWarnings("unchecked")
            P p = (P) instantiate(A, (Label) param);
            return p;
        }
        throw new InternalCompilerError(param.position(),
                "Unexpected subclass of ActsForParam: " + param.getClass());
    }

    public Principal instantiate(JifContext A, Principal p)
            throws SemanticException {
        return JifInstantiator.instantiate(p, A, receiverExpr, calleeContainer,
                receiverLabel,
                getArgLabelsFromFormalTypes(this.pi.formalTypes(),
                        (JifTypeSystem) this.pi.typeSystem(),
                        this.pi.position()),
                pi.formalTypes(), this.actualArgs, this.actualParamLabels);
    }

    public Type instantiate(JifContext A, Type t) throws SemanticException {
        return JifInstantiator.instantiate(t, A, receiverExpr, calleeContainer,
                receiverLabel,
                getArgLabelsFromFormalTypes(pi.formalTypes(),
                        (JifTypeSystem) pi.typeSystem(), pi.position()),
                pi.formalTypes(), this.actualArgLabels, this.actualArgs,
                this.actualParamLabels);
    }

    /**
     * this.pi is a Jif method instance that this.overridingMethod is attempting to
     * override. Previous type checks have made sure that things like
     * abstractness, access flags, throw sets, etc. are ok.
     * We need to check that the labels conform.
     * 
     * @throws SemanticException
     */
    public void checkOverride(LabelChecker lc) throws SemanticException {
        if (!overrideChecker) {
            throw new InternalCompilerError(
                    "Not available for override checking");
        }

        // construct a JifContext here, that equates the arg labels of
        // mi and mj.
        JifContext A = lc.context();
        A = (JifContext) A.pushBlock();
        JifTypeSystem ts = lc.typeSystem();

        final JifMethodInstance overridden = (JifMethodInstance) this.pi;
        final JifMethodInstance overriding = this.overridingMethod;

        if (overriding.formalTypes().size() != overridden.formalTypes()
                .size()) {
            throw new InternalCompilerError("Different number of arguments!");
        }

        LabelChecker newlc = lc.context(A);

        // add the "where caller" authority of the superclass only
        Set<Principal> newAuth = new LinkedHashSet<Principal>();
        JifProcedureDeclExt_c.addCallers(overridden, newAuth);
        A.setAuthority(instantiate(A, newAuth));

        // add the where constraints of the superclass only.
        JifProcedureDeclExt_c.constrainLabelEnv(overridden, newlc.context(),
                this);

        // check that the where constraints of the subclass are implied by
        // those of the superclass.
        satisfiesConstraints(overriding, newlc, false);

        // argument labels and types are contravariant:
        //      each argument label of mi may be more restrictive than the
        //      correponding argument label in mj
        Iterator<? extends Type> miargs = overriding.formalTypes().iterator();
        Iterator<? extends Type> mjargs = overridden.formalTypes().iterator();
        int c = 0;
        while (miargs.hasNext() && mjargs.hasNext()) {
            Type i = miargs.next();
            Type j = mjargs.next();
            ArgLabel ai = (ArgLabel) ts.labelOfType(i);
            ArgLabel aj = (ArgLabel) ts.labelOfType(j);
            final int argIndex = ++c;
            newlc.constrain(
                    new NamedLabel("sup_arg_" + argIndex,
                            "label of " + StringUtil.nth(argIndex)
                                    + " arg of overridden method",
                    instantiate(A, aj.upperBound())),
                    LabelConstraint.LEQ,
                    new NamedLabel("sub_arg_" + argIndex,
                            "label of " + StringUtil.nth(argIndex)
                                    + " arg of overridding method",
                            ai.upperBound()),
                    A.labelEnv(), overriding.position(),
                    new ConstraintMessage() {
                        @Override
                        public String msg() {
                            return "Cannot override " + overridden.signature()
                                    + " in " + overridden.container() + " with "
                                    + overriding.signature() + " in "
                                    + overriding.container()
                                    + ". The label of the "
                                    + StringUtil.nth(argIndex) + " argument "
                                    + "of the overriding method cannot "
                                    + "be less restrictive than in "
                                    + "the overridden method.";

                        }
                    });

            // make sure any parameterized type are in fact subtypes
            new SubtypeChecker(ts.unlabel(i), instantiate(A, ts.unlabel(j)))
                    .addSubtypeConstraints(lc, overriding.position());
        }

        // pc bounds  are contravariant:
        //    the pc bound on mi may be more restrictive than the
        // pc bound on mj
        NamedLabel starti = new NamedLabel(
                "sub_pc_bound", "PC bound of method " + overriding.name()
                        + " in " + overriding.container(),
                overriding.pcBound());
        NamedLabel startj = new NamedLabel("sup_pc_bound",
                "PC bound of method " + overridden.name() + " in "
                        + overridden.container(),
                instantiate(A, overridden.pcBound()));
        newlc.constrain(startj, LabelConstraint.LEQ, starti, A.labelEnv(),
                overriding.position(), new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "Cannot override " + overridden.signature()
                                + " in " + overridden.container() + " with "
                                + overriding.signature() + " in "
                                + overriding.container()
                                + ". The program counter bound of the "
                                + "overriding method "
                                + "cannot be less restrictive than in "
                                + "the overridden method.";

                    }

                    @Override
                    public String detailMsg() {
                        return msg()
                                + " The program counter bound of a method is a lower "
                                + "bound on the observable side effects that "
                                + "the method may perform (such as updates to fields), and "
                                + "an upper bound of the program counter label at the call site.";

                    }
                });

        // return labels are covariant
        //      the return label on mi may be less restrictive than the
        //      return label on mj
        NamedLabel reti = new NamedLabel(
                "sub_return_label", "return label of method "
                        + overriding.name() + " in " + overriding.container(),
                overriding.returnLabel());
        NamedLabel retj = new NamedLabel("sup_return_label",
                "return label of method " + overridden.name() + " in "
                        + overridden.container(),
                instantiate(A, overridden.returnLabel()));
        newlc.constrain(reti, LabelConstraint.LEQ, retj, A.labelEnv(),
                overriding.position(), new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "Cannot override " + overridden.signature()
                                + " in " + overridden.container() + " with "
                                + overriding.signature() + " in "
                                + overriding.container()
                                + ". The return label of the "
                                + "overriding method "
                                + "cannot be more restrictive than in "
                                + "the overridden method.";

                    }

                    @Override
                    public String detailMsg() {
                        return msg()
                                + " The return label of a method is an upper "
                                + "bound on the information that can be gained "
                                + "by observing that the method terminates normally.";

                    }
                });

        // return value labels are covariant
        //      the return value label on mi may be less restrictive than the
        //      return value label on mj
        NamedLabel retVali = new NamedLabel("sub_return_val_label",
                "label of the return value of method " + overriding.name()
                        + " in " + overriding.container(),
                overriding.returnValueLabel());
        NamedLabel retValj = new NamedLabel("sup_return_val_label",
                "label of the return value of method " + overridden.name()
                        + " in " + overridden.container(),
                instantiate(A, overridden.returnValueLabel()));
        newlc.constrain(retVali, LabelConstraint.LEQ, retValj, A.labelEnv(),
                overriding.position(), new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "Cannot override " + overridden.signature()
                                + " in " + overridden.container() + " with "
                                + overriding.signature() + " in "
                                + overriding.container()
                                + ". The return value label of the "
                                + "overriding method "
                                + "cannot be more restrictive than in "
                                + "the overridden method.";

                    }

                    @Override
                    public String detailMsg() {
                        return msg()
                                + " The return value label of a method is the "
                                + "label of the value returned by the method.";

                    }
                });

        // exception labels are covariant
        //          the label of an exception E on mi may be less restrictive
        //          than the label of any exception E' on mj, where E<=E'
        @SuppressWarnings("unchecked")
        List<LabeledType> miExc = (List<LabeledType>) overriding.throwTypes();
        @SuppressWarnings("unchecked")
        List<LabeledType> mjExc = (List<LabeledType>) overridden.throwTypes();

        for (final LabeledType exi : miExc) {

            // find the corresponding exception(s) in mhExc
            for (final LabeledType exj : mjExc) {
                if (ts.isSubtype(exi.typePart(), exj.typePart())) {
                    newlc.constrain(new NamedLabel(
                            "exc_label_" + exi.typePart().toString(), "", //"label on the exception " + exi.typePart().toString(),
                            exi.labelPart()),
                            LabelConstraint.LEQ,
                            new NamedLabel(
                                    "exc_label_" + exj.typePart().toString(),
                                    "", instantiate(A, exj.labelPart())),
                            A.labelEnv(), overriding.position(),
                            new ConstraintMessage() {
                                @Override
                                public String msg() {
                                    return "Cannot override "
                                            + overridden.signature() + " in "
                                            + overridden.container() + " with "
                                            + overriding.signature() + " in "
                                            + overriding.container()
                                            + ". The label of the "
                                            + exi.typePart().toString()
                                            + " exception in overriding method "
                                            + "cannot be more restrictive "
                                            + "than the label of the "
                                            + exj.typePart().toString()
                                            + " exception in "
                                            + "the overridden method.";

                                }

                                @Override
                                public String detailMsg() {
                                    return "Cannot override "
                                            + overridden.signature() + " in "
                                            + overridden.container() + " with "
                                            + overriding.signature() + " in "
                                            + overriding.container()
                                            + ". If the exception "
                                            + exi.typePart().toString()
                                            + " is thrown " + "by "
                                            + overriding.signature() + " in "
                                            + overriding.container()
                                            + " then more information "
                                            + "may be revealed than is permitted by "
                                            + "the overridden method throwing "
                                            + "the exception "
                                            + exj.typePart().toString() + ".";

                                }
                            });
                }
            }
        }

    }

}
