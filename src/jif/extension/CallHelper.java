package jif.extension;

import java.util.*;

import jif.ast.*;
import jif.types.*;
import jif.types.label.*;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.ast.Local;
import polyglot.ast.Receiver;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.StringUtil;

/**
 * This is a tool to label check method calls. This class should be used by
 * creating an instance of it, and then calling the method
 * {@link #checkCall(LabelChecker, List) checkCall(LabelChecker)}. After the
 * call to that method, the remaining methods (which are getter methods) may be
 * called.
 */
public class CallHelper {
    private static boolean shouldReport(int obscurity) {
        return Report.should_report(jif.Topics.labels, obscurity);
    }

    private static void
    report(int obscurity, String s) {
        Report.report(obscurity, "labels: " + s);
    }

    /**
     * Label of the reference to the object on which the procedure is being
     * called.
     */
    private final Label receiverLabel;

    private final Expr receiverExpr;

    private final ReferenceType calleeContainer;

    /**
     * Copy of the list of the <code>Expr</code> s that are the arguments to
     * the procedure call. As we label check the argument expressions, we
     * replace the elements of this list with the label checked versions of the
     * argument expressions.
     */
    private final List actualArgs;

    /**
     * The procedure being called. Also the MethodInstance of the overridden
     * method, when this class is used for overriding checking.
     */
    private final JifProcedureInstance pi;

    /**
     * The position of the procedure call
     */
    private final Position position;

    /**
     * Labels of the actual arguments.
     */
    private List actualArgLabels;

    /**
     * Labels of the actual parameters.
     */
    private List actualParamLabels;

    /**
     * The PathMap for the procedure call.
     */
    private PathMap X;

    /**
     * The return type of the procedure, if there is one.
     */
    private Type returnType;

    /*
     * Flags to ensure that this class is used correctly.
     */
    private boolean callChecked = false;
    private boolean overrideChecker = false; // false if call checker, true if override checker

    /**
     * Method instance of the overriding (subclasses') method. Used
     * only for override checking.
     */
    JifMethodInstance overridingMethod = null;

    public CallHelper(Label receiverLabel,
            Receiver receiver,
            ReferenceType calleeContainer,
            JifProcedureInstance pi,
            List actualArgs,
            Position position) {
        this.receiverLabel = receiverLabel;
        this.calleeContainer = calleeContainer;
        if (receiver instanceof Expr) {
            this.receiverExpr = (Expr)receiver;
        }
        else {
            this.receiverExpr = null;
        }
        this.actualArgs = new ArrayList(actualArgs);
        this.pi = pi;
        this.position = position;
        this.callChecked = false;

        if (pi.formalTypes().size() != actualArgs.size())
            throw new InternalCompilerError("Wrong number of args.");
    }

    public CallHelper(Label receiverLabel, ReferenceType calleeContainer,
            JifProcedureInstance pi, List actualArgs, Position position) {
        this(receiverLabel, null, calleeContainer, pi, actualArgs, position);
    }

    public static CallHelper OverrideHelper(
            JifMethodInstance overridden,
            JifMethodInstance overriding,
            LabelChecker lc) {

        JifTypeSystem jts = (JifTypeSystem)overridden.typeSystem();
        JifNodeFactory nf = (JifNodeFactory)lc.nodeFactory();
        JifClassType subContainer = (JifClassType)overriding.container();
        Label receiverLabel = subContainer.thisLabel();
        Receiver receiver = nf.This(overriding.position());
        ReferenceType calleeContainer = overridden.container().toReference();

        List actualArgs = new ArrayList(overriding.formalTypes().size());

        for (Iterator iter = overriding.formalTypes().iterator(); iter.hasNext(); ) {
            Type t = (Type)iter.next();
            if (jts.isLabeled(t)) {
                ArgLabel al = (ArgLabel)jts.labelOfType(t);
                LocalInstance formalInst = (LocalInstance)al.formalInstance();
                Local l = nf.Local(formalInst.position(), nf.Id(al.position(), al.name())).
                localInstance(formalInst);
                actualArgs.add(l);
            }
            else {
                throw new InternalCompilerError("Formal type is not labeled!");
            }
        }


        CallHelper ch = new CallHelper(receiverLabel, 
                                       receiver, 
                                       calleeContainer,
                                       overridden, 
                                       actualArgs,
                                       overriding.position());
        ch.overrideChecker = true;
        ch.overridingMethod = overriding;
        ch.actualParamLabels = Collections.EMPTY_LIST;
        ch.actualArgLabels = new ArrayList(overriding.formalTypes().size());

        for (Iterator iter = overriding.formalTypes().iterator(); iter.hasNext(); ) {
            Type t = (Type)iter.next();
            ArgLabel al = (ArgLabel)jts.labelOfType(t);
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

    public List labelCheckedArgs() {
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

    private PathMap labelCheckAndConstrainParams(LabelChecker lc, List throwTypes) throws SemanticException {
        PathMap Xjoin;
        JifTypeSystem ts = lc.typeSystem();
        LabelTypeCheckUtil ltcu = ts.labelTypeCheckUtil();

        // If it's a constructor or a static method call, label check
        // the class type, since that may reveal some information.
        // if the method call is a constructor call, or a static method call,
        // then we need to check the pathmap.
        if (this.pi.flags().isStatic()) {
            Xjoin = ltcu.labelCheckType(pi.container(), lc, throwTypes, position);
            List Xparams = ltcu.labelCheckTypeParams(pi.container(), lc, throwTypes, position);
            actualParamLabels = new ArrayList(Xparams.size());
            for (Iterator iter = Xparams.iterator(); iter.hasNext();) {
                PathMap Xj = (PathMap)iter.next();
                actualParamLabels.add(Xj.NV());
            }
        }
        else if (this.pi instanceof ConstructorInstance) {
            Xjoin = ltcu.labelCheckType(pi.container(), lc, throwTypes, position);
            // now constraint params, pretending that they will be args to the constructor with upper bound {this}.
            List Xparams = ltcu.labelCheckTypeParams(pi.container(), lc, throwTypes, position);
            actualParamLabels = new ArrayList(Xparams.size());
            JifContext A = lc.context();

            NamedLabel paramUB = new NamedLabel("param_upper_bound",
                                                "the upper bound on the information that may be revealed by any actual parameter",
                                                this.receiverLabel);

            int counter = 0;
            for (Iterator iter = Xparams.iterator(); iter.hasNext();) {
                PathMap Xj = (PathMap)iter.next();
                actualParamLabels.add(Xj.NV());
                final int count = ++counter;
                lc.constrain(new LabelConstraint(new NamedLabel("actual_param_"+count,
                                                                "the label of the " + StringUtil.nth(count) + " actual parameter",
                                                                Xj.NV()),
                                                                LabelConstraint.LEQ,
                                                                paramUB,
                                                                A.labelEnv(),
                                                                this.position) {
                    public String msg() {
                        return "The actual parameter is more restrictive than " +
                        "permitted.";
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
        }
        else {
            Xjoin = ts.pathMap().N(lc.context().pc());
            actualParamLabels = Collections.EMPTY_LIST;
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
    private PathMap labelCheckAndConstrainArgs(LabelChecker lc, PathMap Xjoin)
    throws SemanticException
    {
        JifContext A = lc.context();
        JifTypeSystem ts = lc.typeSystem();

        // X_0 = X_null[n := A[pc]]
        PathMap Xj = ts.pathMap();
        Xj = Xj.N(A.pc());

        this.actualArgLabels = new ArrayList(this.actualArgs.size());

        for (int i = 0; i < actualArgs.size(); i++) {
            Expr Ej = (Expr)actualArgs.get(i);

            // A[pc := X_{j-1}[N]] |- Ej : Xj
            A = (JifContext)A.pushBlock();
            A.setPc(Xj.N());
            Ej = (Expr)lc.context(A).labelCheck(Ej);
            A = (JifContext)A.pop();

            actualArgs.set(i, Ej);

            Xj = Jif_c.getPathMap(Ej);
            actualArgLabels.add(Xj.NV());
            Xjoin = Xjoin.join(Xj);
        }

        // now that the actualArgs list is correct, we can constrain the args
        for (int i = 0; i < actualArgs.size(); i++) {
            Expr Ej = (Expr)actualArgs.get(i);
            constrainArg(lc, i, Ej, (Type)pi.formalTypes().get(i));
        }

        return Xjoin;
    }

    /**
     * Add constraints to ensure that the labels of the actual arguments
     * are less than the upper bounds of the formal arguments.
     * @param index the ith arg
     * @param Ej the expression of the ith actual arg
     * @param formalType the type of ith formal arg.
     * 
     */
    private void constrainArg(LabelChecker lc, final int index, final Expr Ej, Type formalType) throws SemanticException {
        // constrain the labels of the argument to be less
        // than the bound of the formal arg, substituting in the
        // fresh labels we just created.
        JifContext A = lc.context();
        JifTypeSystem jts = (JifTypeSystem)A.typeSystem();


        ArgLabel aj = (ArgLabel)jts.labelOfType(formalType);

        // the upper bound label of the formal argument
        Label argBoundj = instantiate(A, aj.upperBound());

        // A |- Xj[nv] <= argLj
        PathMap Xj = Jif_c.getPathMap(Ej);
        lc.constrain(new LabelConstraint(new NamedLabel("actual_arg_"+(index+1),
                                                        "the label of the " + StringUtil.nth(index+1) + " actual argument",
                                                        Xj.NV()),
                                                        LabelConstraint.LEQ,
                                                        new NamedLabel("formal_arg_" + (index+1),
                                                                       "the upper bound of the formal argument " + aj.formalInstance().name(),
                                                                       argBoundj),
                                                                       A.labelEnv(),
                                                                       Ej.position()) {
            public String msg() {
                return "The actual argument is more restrictive than " +
                "the formal argument.";
            }

            public String detailMsg() {
                return "The label of the actual argument, " + namedLhs() +
                ", is more restrictive than the label of the " +
                "formal argument, " + namedRhs() + ".";
            }

            public String technicalMsg() {
                return "Invalid argument: the actual argument <" + Ej +
                "> has a more restrictive label than the " +
                "formal argument.";
            }
        }
        );

        // Must check that the actual is a subtype of the formal.
        // Most of this is done in typeCheck, but if actual and formal
        // are instantitation types, we must add constraints for the
        // labels.
        SubtypeChecker sc = new SubtypeChecker(instantiate(A, jts.unlabel(formalType)), jts.unlabel(Ej.type()));
        sc.addSubtypeConstraints(lc, Ej.position());
    }

    /**
     * Make sure that the actual arg for
     * any formal arg that appears in the signature is final.
     * @throws SemanticException 
     *
     */
    private void constrainFinalActualArgs(JifTypeSystem jts) throws SemanticException {
        // find which formal arguments appear in the signature
        final Set argInstances = new LinkedHashSet();
        LabelSubstitution argLabelGather = new LabelSubstitution() {
            public AccessPath substAccessPath(AccessPath ap) {            
                extractRoot(ap.root());
                return ap;
            }            
            void extractRoot(AccessPathRoot root) {
                if (root instanceof AccessPathLocal) {
                    argInstances.add(((AccessPathLocal)root).localInstance());
                }                
            }
        };

        pi.subst(argLabelGather);

        // now go through each of the actual and formal arguments, and
        // check if the actual arg needs to be final. 
        Iterator formalTypes = pi.formalTypes().iterator();
        for (int j = 0; j < actualArgs.size(); j++) {
            Type tj = (Type)formalTypes.next();
            ArgLabel aj = (ArgLabel)jts.labelOfType(tj);
            if (argInstances.contains(aj.formalInstance())) {
                // this actual arg needs to be final!
                Expr Ej = (Expr)actualArgs.get(j);
                if (!JifUtil.isFinalAccessExprOrConst(jts, Ej)) {
                    throw new SemanticDetailedException("The " + 
                                                        StringUtil.nth(j+1) + 
                                                        " argument must be a final access path or a " +
                                                        "constant", 
                                                        "The " + StringUtil.nth(j+1) + " formal argument " +
                                                        "of " + pi.debugString() + 
                                                        " is used to express a dynamic label or principal " +
                                                        "in the procedure's signature. As such, " +
                                                        "the actual argument must be a final " +
                                                        "access path or a constant.",   
                                                        Ej.position());
                }
            }

        }               
    }

    private Label resolvePCBound(LabelChecker lc) throws SemanticException {
        return instantiate(lc.context(), pi.pcBound());
    }

    /**
     * Returns the instantiated return label.
     * @throws SemanticException
     */
    private Label resolveReturnLabel(LabelChecker lc) throws SemanticException {
        return instantiate(lc.context(), pi.returnLabel());
    }

    /**
     * Returns the instantiated return value label joined with returnLabel.
     * @throws SemanticException
     */
    private Label resolveReturnValueLabel(LabelChecker lc, Label returnLabel) throws SemanticException {
        JifTypeSystem ts = lc.typeSystem();
        Label L = null;

        if (pi instanceof MethodInstance) {
            MethodInstance mi = (MethodInstance)pi;
            L = instantiate(lc.context(), ts.labelOfType(mi.returnType()));
        }
        else {
            L = ts.bottomLabel(pi.position());
        }

        return lc.upperBound(L, returnLabel);
    }

    private PathMap excPathMap(LabelChecker lc, Label returnLabel, Label pcPriorToInvoke, List throwTypes) throws SemanticException {
        JifTypeSystem ts = lc.typeSystem();
        PathMap Xexn = ts.pathMap();

        for (Iterator e = pi.throwTypes().iterator(); e.hasNext();) {
            Type te = (Type)e.next();
            Label Le = ts.labelOfType(te, returnLabel);
            Le = instantiate(lc.context(), Le);
            Jif_c.checkAndRemoveThrowType(throwTypes, te);
            Xexn = Xexn.exception(te, lc.upperBound(Le, pcPriorToInvoke));
        }

        return Xexn;
    }

    /**
     * Check method calls. (Thesis, Figure 4.29)
     * 
     *  
     */
    public void checkCall(LabelChecker lc, List throwTypes, boolean targetMayBeNull)
    throws SemanticException
    {
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
        lc = lc.context((JifContext)lc.context().pushBlock());        
        lc.context().setPc(Xjoin.N());

        // check arguments
        Xjoin = labelCheckAndConstrainArgs(lc, Xjoin);
        if (targetMayBeNull) {
            // a null pointer exception may be thrown
            Jif_c.checkAndRemoveThrowType(throwTypes, ts.NullPointerException());
            Xjoin = Xjoin.exc(receiverLabel, ts.NullPointerException());            
        }        
        constrainFinalActualArgs(ts);
        lc = lc.context((JifContext)lc.context().pushBlock());        
        lc.context().setPc(Xjoin.N());

        // A |- X_{maxj}[N] + entry_pc <= Li
        Label Li = resolvePCBound(lc);
        if (Li != null) {
            final ProcedureInstance callee = pi;
            JifContext A = lc.context();

            // the path map for the last argument.
            NamedLabel namedLi = new NamedLabel("callee_PC_bound",
                                                "lower bound on the side effects of the method " + callee.signature(),
                                                Li);

            lc.constrain(new LabelConstraint(new NamedLabel("pc_call",
                                                            "label of the program counter at this call site",
                                                            Xjoin.N()),
                                                            LabelConstraint.LEQ,
                                                            namedLi,
                                                            A.labelEnv(),
                                                            position) {
                public String msg() {
                    return "PC at call site more restrictive than " +
                    "begin label of " + callee.signature() + ".";
                }

                public String detailMsg() {
                    return "Calling the method at this program point may " +
                    "reveal too much information to the receiver of " +
                    "the method call. " + callee.signature() + " can only be invoked " +
                    "if the invocation will reveal no more " +
                    "information than the callee's begin label, " +
                    namedRhs() + ". However, execution reaching " +
                    "this program point may depend on information " +
                    "up to the PC at this program point: " +
                    namedLhs() + ".";
                }

                public String technicalMsg() {
                    return "Invalid method call: " + namedLhs() +
                    " is more restrictive than " +
                    "the callee's begin label.";
                }
            }
            );
            lc.constrain(new LabelConstraint(new NamedLabel("caller_PC_bound",
                                                            "lower bound on the side effects of caller",
                                                            A.currentCodePCBound()),
                                                            LabelConstraint.LEQ,
                                                            namedLi,
                                                            A.labelEnv(),
                                                            position) {
                public String msg() {
                    return "The side effects of " + callee.signature() +
                    " are not bounded by the PC bound.";
                }

                public String detailMsg() {
                    return "Calling the method here may have side effects " +
                    "that are not bounded below by the PC bound of the " +
                    "caller. The side effects of the method to be " +
                    "invoked are bounded below by the callee's " +
                    "begin label, " + namedRhs() +
                    ". However, the side effects of the calling context must " +
                    "be bounded below by the caller's begin label " +
                    namedLhs() + ".";
                }

                public String technicalMsg() {
                    return "Invalid method call: " + namedLhs() +
                    " is more restrictive than " +
                    "the callee's begin label.";
                }
            }
            );
        }

        satisfiesConstraints(pi, lc);

        // A |- call-end(ct, pi, Aa, Li, LrvDef)
        if (shouldReport( 4))
            report( 4, ">>>>> call-end");

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
            returnType = instantiate(lc.context(), ((MethodInstance)pi).returnType());
        }
        else {
            returnType = null;
        }

        // record that this method has now been called.
        callChecked = true;
    }

    /** Check if the caller has sufficient authority, and label constraints
     * are satisfied
     *  Thesis, Figure 4.29
     */
    private void satisfiesConstraints(final JifProcedureInstance jpi, LabelChecker lc)
    throws SemanticException
    {
        JifContext A = lc.context();

        for (Iterator i = jpi.constraints().iterator(); i.hasNext();) {
            Assertion jc = (Assertion)i.next();

            if (jc instanceof AuthConstraint || jc instanceof AutoEndorseConstraint) {
                continue;
            }
            else if (jc instanceof CallerConstraint) {
                CallerConstraint jcc = (CallerConstraint)jc;

                // Check the authority
                for (Iterator i2 = jcc.principals().iterator(); i2.hasNext();) {
                    Principal pi = (Principal)i2.next();
                    pi = instantiate(A, pi);

                    boolean sat = false;

                    for (Iterator j = A.authority().iterator(); j.hasNext();) {
                        Principal pj = (Principal)j.next();

                        if (A.actsFor(pj, pi)) {
                            sat = true;
                            break;
                        }
                    }

                    if (!sat) {
                        if (!overrideChecker) {
                            // being used as a normal call checker
                            throw new SemanticDetailedException(
                                                                "The caller must have the authority of the principal " + 
                                                                pi + " to invoke " + jpi.debugString(),
                                                                "The " + jpi.debugString() + " requires that the " +
                                                                "caller of the method have the authority of " +
                                                                "the principal "  + pi +". The caller does " +
                                                                "not have this principal's authority.", 
                                                                position);
                        }
                        else {
                            // being used as an override checker
                            throw new SemanticDetailedException(
                                                                "The subclass cannot assume the caller has the " +
                                                                "authority of the principal " + 
                                                                pi,
                                                                "The " + jpi.debugString() + " requires that the " +
                                                                "caller of the method have the authority of " +
                                                                "the principal "  + pi +". However, this " +
                                                                "method overrides the method in class " + this.pi.container() + 
                                                                " which does not make this requirement.", 
                                                                jcc.position());                            
                        }
                    }
                }
            }
            else if (jc instanceof ActsForConstraint) {
                ActsForConstraint jac = (ActsForConstraint)jc;

                Principal actor = jac.actor();
                actor = instantiate(A, actor);

                Principal granter = jac.granter();
                granter = instantiate(A, granter);

                if (jac.isEquiv()) {
                    if (!A.equiv(actor, granter)) {
                        if (!overrideChecker) {
                            // being used as a normal call checker
                            throw new SemanticDetailedException(
                                                                "The principal " + actor + " must be equivalent to " + 
                                                                granter + " to invoke " + jpi.debugString(),
                                                                "The " + jpi.debugString() + " requires that the " +
                                                                " relationship " + jac + " holds at the call site.", 
                                                                position);
                        }
                        else { 
                            // being used as an override checker
                            throw new SemanticDetailedException(
                                                                "The subclass cannot assume that " + actor + " is " +
                                                                "equivalent to " + granter,
                                                                "The " + jpi.debugString() + " requires that " + actor + " is " +
                                                                "equivalent to " + granter + ". However, this " +
                                                                "method overrides the method in class " + this.pi.container() + 
                                                                " which does not make this requirement.", 
                                                                jac.position());                            
                        }                    
                    }
                }
                else {
                    if (!A.actsFor(actor, granter)) {
                        if (!overrideChecker) {
                            // being used as a normal call checker
                            throw new SemanticDetailedException(
                                                                "The principal " + actor + " must act for " + granter + 
                                                                " to invoke " + jpi.debugString(),
                                                                "The " + jpi.debugString() + " requires that the " +
                                                                "relationship " + actor + " actsfor " + granter + 
                                                                " holds at the call site.", 
                                                                position);
                        }   
                        else { 
                            throw new SemanticDetailedException(
                                                                "The subclass cannot assume that " + actor + " can " +
                                                                "actfor " + granter,
                                                                "The " + jpi.debugString() + " requires that " + actor + " can " +
                                                                "actfor " + granter + ". However, this " +
                                                                "method overrides the method in class " + this.pi.container() + 
                                                                " which does not make this requirement.", 
                                                                jac.position());                            
                        }
                    }
                }
            }
            else if (jc instanceof LabelLeAssertion) {
                LabelLeAssertion lla = (LabelLeAssertion)jc;

                final Label lhs = instantiate(A, lla.lhs());
                final Label rhs = instantiate(A, lla.rhs());
                final String message;
                final String detailedMessage;
                Position pos = position;
                if (!overrideChecker) {
                    // being used as a normal call checker
                    message = "The label " + lhs + " must be less restrictive " +
                    "than " + rhs +" to invoke " + jpi.debugString();
                    detailedMessage = "The " + jpi.debugString() + " requires that the " +
                    "relationship " + lhs + " <= " + rhs + 
                    " holds at the call site.";                    
                }
                else {
                    // being used as an override checker
                    message = "The subclass cannot assume that " + lhs +
                    " <= " + rhs;
                    detailedMessage = "The " + jpi.debugString() + " requires that " + lhs + 
                    " <= " + rhs + ". However, this " +
                    "method overrides the method in class " + this.pi.container() + 
                    " which does not make this requirement.";
                    pos = lla.position();
                }


                lc.constrain(new LabelConstraint(new NamedLabel(lla.lhs().toString(),
                                                                "LHS of label assertion",
                                                                lhs),
                                                                LabelConstraint.LEQ,
                                                                new NamedLabel(lla.rhs().toString(),
                                                                               "RHS of label assertion",
                                                                               rhs),
                                                                               A.labelEnv(),
                                                                               pos) {
                    public String msg() {
                        return message;
                    }

                    public String detailMsg() {
                        return detailedMessage;
                    }
                });
            }
        }
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
            List actualArgVarLabels, List actualParamVarLabels)
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
            lc.constrain(new LabelConstraint(new NamedLabel(receiverVarLabel.componentString(), receiverVarLabel), 
                                             LabelConstraint.EQUAL,
                                             new NamedLabel(receiverVarLabel.componentString(),
                                                            this.receiverLabel), 
                                                            A.labelEnv(), 
                                                            this.position, 
                                                            false));
        }
        else if (receiverVarLabel != null || this.receiverLabel != null) {
            throw new InternalCompilerError("Inconsistent receiver labels", position);
        }

        // bind all the actual arg var labels
        for (int i = 0; i < actualArgLabels.size(); i++) {
            VarLabel argVarLbl = (VarLabel)actualArgVarLabels.get(i);
            Label argLbl = (Label)this.actualArgLabels.get(i);
            lc.constrain(new LabelConstraint(new NamedLabel(argVarLbl.componentString(), argVarLbl), 
                                             LabelConstraint.EQUAL,
                                             new NamedLabel(argVarLbl.componentString(), argLbl), 
                                             A.labelEnv(), this.position, 
                                             false));
        }

        // bind all the actual param var labels.
        // that is, the labels of the parameter values in the type are bound
        // to the variables that represent them.
        // e.g., in the constructor call "new C[lbl1, o.lbl2]()", the values
        // of the expression "lbl1" and "o.lbl2" may reveal information, and
        // thus the labels of the expressions need to be considered. This only
        // needs to be done for static methods and constructor calls.

        if (this.pi.flags().isStatic() || this.pi instanceof ConstructorInstance) {
            for (int i = 0; i < actualParamVarLabels.size(); i++) {
                VarLabel paramVarLbl = (VarLabel)actualParamVarLabels.get(i);
                Label paramLbl = (Label)this.actualParamLabels.get(i);
                lc.constrain(new LabelConstraint(new NamedLabel(paramVarLbl.componentString(), paramVarLbl), 
                                                 LabelConstraint.EQUAL, new NamedLabel(paramVarLbl.componentString(), paramLbl), 
                                                 A.labelEnv(), this.position, 
                                                 false));
            }
        }
    }

    protected static List getArgLabelsFromFormalTypes(List formalTypes,
            JifTypeSystem jts, Position pos) throws SemanticException {
        List formalArgLabels = new ArrayList(formalTypes.size());
        for (Iterator iter = formalTypes.iterator(); iter.hasNext();) {
            Type t = (Type)iter.next();
            Label l = jts.labelOfType(t);
            if (!(l instanceof ArgLabel)) {
                throw new SemanticException("Internal label error, probably caused by an earlier error.", pos);
            }
            ArgLabel al = (ArgLabel)l;
            formalArgLabels.add(al);
        }
        return formalArgLabels;
    }

    public Label instantiate(JifContext A, Label L) throws SemanticException {
        return JifInstantiator.instantiate(L, A, receiverExpr, calleeContainer, receiverLabel,
                                           getArgLabelsFromFormalTypes(pi.formalTypes(), (JifTypeSystem)pi.typeSystem(), pi.position()),
                                           this.actualArgLabels,
                                           this.actualArgs,
                                           this.actualParamLabels);
    }

    /**
     * replaces any signature ArgLabels in p with the appropriate label, and
     * replaces any signature ArgPrincipal with the appropriate prinicipal.
     * @throws SemanticException
     */
    public Principal instantiate(JifContext A, Principal p) throws SemanticException {
        return JifInstantiator.instantiate(p, A, receiverExpr, calleeContainer, receiverLabel,
                                           getArgLabelsFromFormalTypes(this.pi.formalTypes(), (JifTypeSystem)this.pi.typeSystem(), this.pi.position()),
                                           this.actualArgs,
                                           this.actualParamLabels);
    }

    public Type instantiate(JifContext A, Type t) throws SemanticException {
        return JifInstantiator.instantiate(t, A, receiverExpr, calleeContainer, receiverLabel,
                                           getArgLabelsFromFormalTypes(pi.formalTypes(), (JifTypeSystem)pi.typeSystem(), pi.position()),
                                           this.actualArgLabels,
                                           this.actualArgs,
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
            throw new InternalCompilerError("Not available for override checking");            
        }

        // construct a JifContext here, that equates the arg labels of
        // mi and mj.
        JifContext A = lc.context(); 
        A = (JifContext) A.pushBlock();
        JifTypeSystem ts = lc.typeSystem();

        final JifMethodInstance overridden = (JifMethodInstance)this.pi;
        final JifMethodInstance overriding = this.overridingMethod;

        if (overriding.formalTypes().size() != overridden.formalTypes().size()) {
            throw new InternalCompilerError("Different number of arguments!");
        }


        LabelChecker newlc = lc.context(A);

        // add the "where caller" authority of the superclass only
        Set newAuth = new LinkedHashSet();
        JifProcedureDeclExt_c.addCallers(overridden, newlc.context(), newAuth);
        A.setAuthority(newAuth);       

        // add the where constraints of the superclass only.
        JifProcedureDeclExt_c.constrainLabelEnv(overridden, newlc.context(), this);        

        // check that the where constraints of the subclass are implied by 
        // those of the superclass.
        satisfiesConstraints(overriding, newlc);

        // argument labels and types are contravariant:
        //      each argument label of mi may be more restrictive than the 
        //      correponding argument label in mj        
        Iterator miargs = overriding.formalTypes().iterator();
        Iterator mjargs = overridden.formalTypes().iterator();
        int c=0;
        while (miargs.hasNext() && mjargs.hasNext()) {
            Type i = (Type)miargs.next();
            Type j = (Type)mjargs.next();
            ArgLabel ai = (ArgLabel)ts.labelOfType(i);
            ArgLabel aj = (ArgLabel)ts.labelOfType(j);
            final int argIndex = ++c;
            newlc.constrain(new LabelConstraint(new NamedLabel("sup_arg_"+argIndex,
                                                               "label of " + StringUtil.nth(argIndex) + " arg of overridden method",
                                                               instantiate(A, aj.upperBound())),
                                                               LabelConstraint.LEQ,
                                                               new NamedLabel("sub_arg_"+argIndex,
                                                                              "label of " + StringUtil.nth(argIndex) + " arg of overridding method",
                                                                              ai.upperBound()),
                                                                              A.labelEnv(),
                                                                              overriding.position()) {
                public String msg() {
                    return "Cannot override " + overridden.signature() + 
                    " in " + overridden.container() + " with " + 
                    overriding.signature() + " in " + 
                    overriding.container() + ". The label of the " + 
                    StringUtil.nth(argIndex) + " argument " +
                    "of the overriding method cannot " +
                    "be less restrictive than in " +
                    "the overridden method.";                

                }
            }
            );

            // make sure any parameterized type are in fact subtypes
            new SubtypeChecker(ts.unlabel(i), 
                               instantiate(A, ts.unlabel(j))).addSubtypeConstraints(lc, 
                                                       overriding.position());
        }


        // pc bounds  are contravariant:
        //    the pc bound on mi may be more restrictive than the 
        // pc bound on mj
        NamedLabel starti = new NamedLabel("sub_pc_bound",
                                           "PC bound of method " + overriding.name() + " in " + overriding.container(), 
                                           overriding.pcBound());
        NamedLabel startj = new NamedLabel("sup_pc_bound",
                                           "PC bound of method " + overridden.name() + " in " + overridden.container(), 
                                           instantiate(A, overridden.pcBound()));
        newlc.constrain(new LabelConstraint(startj,
                                            LabelConstraint.LEQ,
                                            starti,
                                            A.labelEnv(),
                                            overriding.position()) {
            public String msg() {
                return "Cannot override " + overridden.signature() + 
                " in " + overridden.container() + " with " + 
                overriding.signature() + " in " + 
                overriding.container() + ". The program counter bound of the " + 
                "overriding method " +
                "cannot be less restrictive than in " +
                "the overridden method.";                

            }
            public String detailMsg() {
                return msg() + 
                " The program counter bound of a method is a lower " +
                "bound on the observable side effects that " +
                "the method may perform (such as updates to fields), and " +
                "an upper bound of the program counter label at the call site.";                

            }
        }
        );

        // return labels are covariant
        //      the return label on mi may be less restrictive than the
        //      return label on mj
        NamedLabel reti = new NamedLabel("sub_return_label", 
                                         "return label of method " + overriding.name() + " in " + overriding.container(), 
                                         overriding.returnLabel());
        NamedLabel retj = new NamedLabel("sup_return_label", 
                                         "return label of method " + overridden.name() + " in " + overridden.container(), 
                                         instantiate(A, overridden.returnLabel()));                        
        newlc.constrain(new LabelConstraint(reti,
                                            LabelConstraint.LEQ,
                                            retj,
                                            A.labelEnv(),
                                            overriding.position()) {
            public String msg() {
                return "Cannot override " + overridden.signature() + 
                " in " + overridden.container() + " with " + 
                overriding.signature() + " in " + 
                overriding.container() + ". The return label of the " + 
                "overriding method " +
                "cannot be more restrictive than in " +
                "the overridden method.";                

            }
            public String detailMsg() {
                return msg() + 
                " The return label of a method is an upper " +
                "bound on the information that can be gained " +
                "by observing that the method terminates normally.";                

            }
        }
        );


        // return value labels are covariant
        //      the return value label on mi may be less restrictive than the
        //      return value label on mj
        NamedLabel retVali = new NamedLabel("sub_return_val_label",
                                            "label of the return value of method " + overriding.name() + " in " + overriding.container(), 
                                            overriding.returnValueLabel());
        NamedLabel retValj = new NamedLabel("sup_return_val_label", 
                                            "label of the return value of method " + overridden.name() + " in " + overridden.container(), 
                                            instantiate(A, overridden.returnValueLabel()));
        newlc.constrain(new LabelConstraint(retVali,
                                            LabelConstraint.LEQ,
                                            retValj,
                                            A.labelEnv(),
                                            overriding.position()) {
            public String msg() {
                return "Cannot override " + overridden.signature() + 
                " in " + overridden.container() + " with " + 
                overriding.signature() + " in " + 
                overriding.container() + ". The return value label of the " + 
                "overriding method " +
                "cannot be more restrictive than in " +
                "the overridden method.";                

            }
            public String detailMsg() {
                return msg() + 
                " The return value label of a method is the " +
                "label of the value returned by the method.";                

            }
        }
        );

        // exception labels are covariant
        //          the label of an exception E on mi may be less restrictive
        //          than the label of any exception E' on mj, where E<=E'
        Iterator miExc = overriding.throwTypes().iterator();
        List mjExc = overridden.throwTypes();

        while (miExc.hasNext()) {
            final LabeledType exi = (LabeledType)miExc.next();

            // find the corresponding exception(s) in mhExc
            for (Iterator mjExcIt = mjExc.iterator(); mjExcIt.hasNext(); ) {
                final LabeledType exj = (LabeledType)mjExcIt.next();
                if (ts.isSubtype(exi.typePart(), exj.typePart())) {
                    newlc.constrain(new LabelConstraint(new NamedLabel("exc_label_"+exi.typePart().toString(),
                                                                       "",//"label on the exception " + exi.typePart().toString(),
                                                                       exi.labelPart()),
                                                                       LabelConstraint.LEQ,
                                                                       new NamedLabel("exc_label_"+exj.typePart().toString(),
                                                                                      "",
                                                                                      instantiate(A, exj.labelPart())),
                                                                                      A.labelEnv(),
                                                                                      overriding.position()) {
                        public String msg() {
                            return "Cannot override " + overridden.signature() + 
                            " in " + overridden.container() + " with " + 
                            overriding.signature() + " in " + 
                            overriding.container() + ". The label of the " + 
                            exi.typePart().toString() + 
                            " exception in overriding method " +
                            "cannot be more restrictive " +
                            "than the label of the " + 
                            exj.typePart().toString() +
                            " exception in " +
                            "the overridden method.";                

                        }
                        public String detailMsg() {
                            return "Cannot override " + overridden.signature() + 
                            " in " + overridden.container() + " with " + 
                            overriding.signature() + " in " + 
                            overriding.container() + ". If the exception " +
                            exi.typePart().toString() + " is thrown " +
                            "by " + overriding.signature() + " in " + 
                            overriding.container() + " then more information " +
                            "may be revealed than is permitted by " +
                            "the overridden method throwing " +
                            "the exception " + 
                            exj.typePart().toString() + ".";                

                        }
                    }
                    );
                }
            }
        }

    }

}