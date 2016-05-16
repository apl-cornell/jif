package jif.visit;

import java.util.List;

import jif.ast.JifClassDecl;
import jif.ast.JifMethodDecl;
import jif.ast.JifUtil;
import jif.extension.CallHelper;
import jif.types.Constraint;
import jif.types.ConstraintMessage;
import jif.types.JifContext;
import jif.types.JifMethodInstance;
import jif.types.JifProcedureInstance;
import jif.types.JifTypeSystem;
import jif.types.LabelConstraint;
import jif.types.NamedLabel;
import jif.types.Path;
import jif.types.PrincipalConstraint;
import jif.types.Solver;
import jif.types.hierarchy.LabelEnv;
import jif.types.label.Label;
import jif.types.principal.Principal;

import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Receiver;
import polyglot.frontend.Job;
import polyglot.types.ClassType;
import polyglot.types.MethodInstance;
import polyglot.types.ReferenceType;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.Copy;
import polyglot.util.ErrorInfo;
import polyglot.util.ErrorQueue;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/**
 * The <code>LabelChecker</code> class is used in the label checking of
 * Jif. Primarily it provides the method {@link #labelCheck(Node)
 * labelCheck(Node)} which invokes the {@link jif.ast.JifExt#labelCheck labelCheck}
 * method on nodes, utility functions to help in the implementation of the
 * <code>labelCheck</code> methods, as well as references to the appropriate
 *  {@link jif.types.Solver Solver} and {@link jif.types.JifContext JifContext}.
 * 
 * <p>
 * <code>LabelChecker</code> is mostly imperative; however, the
 * <code>JifContext</code> is treated functionally, and whenever a
 * <code>LabelChecker</code> is given a new <code>JifContext</code>, a new
 *  <code>LabelChecker</code> is created.
 */
public class LabelChecker implements Copy {
    /**
     * The <code>JifContext</code> appropriate for this label checker. The
     * Jif context records a substantial amount of information required for
     * label checking.
     */
    private JifContext context;

    final protected JifTypeSystem ts;
    final protected Job job;
    final protected NodeFactory nf;

    /**
     * If true, then warnings will be produced; otherwise, warnings will be
     * silenced.
     */
    final protected boolean warningsEnabled;

    /**
     * If true, then a new system of constraints will be used for each
     * class body, and upon leaving the class body, the system of constraints
     * will be solved.
     */
    final protected boolean solvePerClassBody;

    /**
     * If true, then a new system of constraints will be used for each
     * method body, and upon leaving the method body, the system of constraints
     * will be solved.
     */
    final protected boolean solvePerMethod;

    /**
     * The <code>Solver</code> to add constraints to. Depending on
     * <code>solveClassBodies</code>, a new <code>Solver</code> is used
     * for every class, or a single <code>Solver</code> used for the entire
     * compilation.
     */
    protected Solver solver;

    public LabelChecker(Job job, TypeSystem ts, NodeFactory nf,
            boolean warningsEnabled, boolean solvePerClassBody,
            boolean solvePerMethod, boolean doLabelSubst) {
        this.job = job;
        this.ts = (JifTypeSystem) ts;
        this.context = (JifContext) ts.createContext();
        this.nf = nf;

        this.warningsEnabled = warningsEnabled;
        this.solvePerClassBody = solvePerClassBody;
        this.solvePerMethod = solvePerMethod;
        //        if (solvePerMethod && solvePerClassBody) {
        //            throw new InternalCompilerError("cant solve for both class and method!");
        //        }
        if (!solvePerClassBody && !solvePerMethod) {
            this.solver = this.ts.createSolver("Job solver: " + job.toString());
        }
    }

    @Override
    public Object copy() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalCompilerError("Java clone() weirdness.", e);
        }
    }

    public JifContext context() {
        return context;
    }

    public JifContext jifContext() {
        return context;
    }

    public LabelChecker context(JifContext c) {
        if (c == this.context) return this;
        LabelChecker lc = (LabelChecker) copy();
        lc.context = c;
        return lc;
    }

    public JifTypeSystem typeSystem() {
        return ts;
    }

    public JifTypeSystem jifTypeSystem() {
        return ts;
    }

    public NodeFactory nodeFactory() {
        return nf;
    }

    public Solver solver() {
        return this.solver;
    }

    public Job job() {
        return this.job;
    }

    public boolean warningsEnabled() {
        return warningsEnabled;
    }

    public ErrorQueue errorQueue() {
        return job.compiler().errorQueue();
    }

    /**
     * Returns an upper bound for L1 and L2
     */
    public Label upperBound(Label L1, Label L2) {
        return ts.join(L1, L2);
    }

    public Label upperBound(Label L1, Label L2, Label L3) {
        return ts.join(ts.join(L1, L2), L3);
    }

    /**
     * Returns a lower bound for L1 and L2
     */
    public Label lowerBound(Label L1, Label L2) {
        return ts.meet(L1, L2);
    }

    protected Node preLabelCheck(Node n) {
        return n;
    }

    protected Node postLabelCheck(Node old, Node n) {
        return n;
    }

    public Node labelCheck(Node n) throws SemanticException {
        if (JifUtil.jifExt(n) != null) {
            this.context().labelEnv().setSolver(this.solver());
            n = preLabelCheck(n);
            Node newNode = JifUtil.jifExt(n).labelCheck(this);
            newNode = postLabelCheck(n, newNode);
            n = newNode;
        }
        return n;
    }

    public void constrain(NamedLabel lhs, LabelConstraint.Kind kind,
            NamedLabel rhs, LabelEnv env, Position pos, ConstraintMessage msg)
                    throws SemanticException {
        constrain(lhs, kind, rhs, env, pos, true, msg);
    }

    public void constrain(NamedLabel lhs, LabelConstraint.Kind kind,
            NamedLabel rhs, LabelEnv env, Position pos, boolean report,
            ConstraintMessage msg) throws SemanticException {
        LabelConstraint c =
                new LabelConstraint(lhs, kind, rhs, env, pos, msg, report);
        if (msg != null) msg.setConstraint(c);
        constrain(c);
    }

    public void constrain(NamedLabel lhs, LabelConstraint.Kind kind,
            NamedLabel rhs, LabelEnv env, Position pos)
                    throws SemanticException {
        constrain(lhs, kind, rhs, env, pos, false, null);
    }

    protected void constrain(Constraint c) throws SemanticException {
        this.solver.addConstraint(c);
    }

    public void constrain(Principal p, Constraint.Kind kind, Principal q,
            LabelEnv env, Position pos, ConstraintMessage msg)
                    throws SemanticException {
        constrain(p, kind, q, env, pos, msg, true);
    }

    public void constrain(Principal p, Constraint.Kind kind, Principal q,
            LabelEnv env, Position pos, ConstraintMessage msg, boolean report)
                    throws SemanticException {
        PrincipalConstraint c =
                new PrincipalConstraint(p, kind, q, env, pos, msg, report);
        if (msg != null) msg.setConstraint(c);
        constrain(c);
    }

    /**
     * Adds a constraint to the solver, specifying that the given label must
     * actfor the given principal.
     */
    public void constrain(NamedLabel label, Principal p, LabelEnv env,
            Position pos, ConstraintMessage msg) throws SemanticException {
        constrain(label, p, env, pos, msg, true);
    }

    /**
     * Adds a constraint to the solver, specifying that the given label must
     * actfor the given principal.
     */
    public void constrain(NamedLabel label, Principal p, LabelEnv env,
            Position pos, ConstraintMessage msg, boolean report)
                    throws SemanticException {
        NamedLabel principalLabel = new NamedLabel(p.toString(),
                "RHS of actsfor constraint", p.typeSystem().toLabel(p));
        constrain(label, LabelConstraint.LEQ, principalLabel, env, pos, report,
                msg);
    }

    /**
     * Called by JifClassDeclExt just before this label checker is used to
     * check a class body. This allows us to use a different solver if
     * required.
     */
    public void enteringClassDecl(ClassType ct) {
        if (solvePerClassBody) {
            // solving by class. Set a new solver for the class body
            this.solver = ts.createSolver(ct.name());
        }
    }

    /**
     * Called by JifMethodDeclExt just before this label checker is used to
     * check a method body. This allows us to use a different solver if
     * required.
     */
    public void enteringMethod(MethodInstance mi) {
        if (solvePerMethod) {
            // solving by method. Set a new solver for the method body
            this.solver = ts
                    .createSolver(mi.container().toString() + "." + mi.name());
        }
    }

    /**
     * Called by JifClassDeclExt just after this label checker has been used to
     * check a class body. This allows us to use a different solver if
     * required.
     */
    public JifClassDecl leavingClassDecl(JifClassDecl n) {
        if (solvePerClassBody) {
            // solving by class. We need to solve the constraints
            return (JifClassDecl) solveConstraints(n);
        }
        return n;
    }

    /**
     * Called by JifClassDeclExt just after this label checker has been used to
     * check a method body. This allows us to use a different solver if
     * required.
     */
    public JifMethodDecl leavingMethod(JifMethodDecl n) {
        if (solvePerMethod) {
            // solving by method. We need to solve the constraints
            return (JifMethodDecl) solveConstraints(n);
        }
        return n;
    }

    /**
     * This method should be called on the top level label checker once
     * the label checking has finished. This will perform label substitution
     * if required.
     */
    public Node finishedLabelCheckPass(Node n) {
        if (!solvePerClassBody) {
            // solving globally. We need to solve the constraints
            return solveConstraints(n);
        }
        return n;
    }

    /**
     * Create a new JifLabelSubst.  Abstracted out so we can override what
     * implementation we're using for it.
     */
    protected JifLabelSubst labelSubst() {
        JifLabelSubst jls =
                new JifLabelSubst(this.job, this.ts, this.nf, this.solver);
        return jls;
    }

    protected Node solveConstraints(Node n) {
        Node newN = n;
        JifLabelSubst jls = labelSubst();

        jls = (JifLabelSubst) jls.begin();

        if (jls != null) {
            newN = n.visit(jls);
            jls.finish(newN);
        }
        return newN;
    }

    public void reportSemanticException(SemanticException e) {
        Position pos =
                e.position() != null ? e.position() : job().ast().position();
        errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, e.getMessage(), pos);
    }

    public CallHelper createCallHelper(Label receiverLabel, Receiver receiver,
            ReferenceType calleeContainer, JifProcedureInstance pi,
            List<Expr> actualArgs, Position position) {
        return new CallHelper(receiverLabel, receiver, calleeContainer, pi,
                actualArgs, position);
    }

    public CallHelper createCallHelper(Label receiverLabel,
            ReferenceType calleeContainer, JifProcedureInstance pi,
            List<Expr> actualArgs, Position position) {
        return createCallHelper(receiverLabel, null, calleeContainer, pi,
                actualArgs, position);
    }

    public CallHelper createOverrideHelper(JifMethodInstance overridden,
            JifMethodInstance overriding) {
        return CallHelper.OverrideHelper(overridden, overriding, this);
    }

    /**
     * Helper function that can be overriden to indicate if a path is to be
     * ignored for the single path rule (such as the NV path).
     */
    public boolean ignoredForSinglePathRule(Path p) {
      return p.equals(Path.NV);
    }
}
