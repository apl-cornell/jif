package jif.types;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jif.JifOptions;
import jif.types.label.Variable;
import polyglot.main.Options;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;
import polyglot.util.SerialVersionUID;

/**
 * Exception indicating that a program constraint is not satisfiable.
 */
public class UnsatisfiableConstraintException extends SemanticException {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected final AbstractSolver solver;
    protected final Equation failure;
    protected final FailedConstraintSnapshot snapshot;

    /**
     * Construct a new UnsatisfiableConstraintException.
     * @param solver
     *          The solver that was used to determine that eqn is unsatisfiable
     * @param eqn
     *          The unsatisfiable equation
     */
    public UnsatisfiableConstraintException(AbstractSolver solver, Equation eqn,
            FailedConstraintSnapshot snapshot) {
        super(eqn.position());
        this.solver = solver;
        this.failure = eqn;
        this.snapshot = snapshot;
    }

    /**
     * Report the traces for each variables in the collection
     * <code>Variables</code>
     */
    protected void reportTraces(Collection<Variable> variables) {
        for (Variable v : variables)
            solver.reportTrace(v);
    }

    /**
     * Produce an error message for the constraint c, which cannot be satisfied.
     */
    public final FailedConstraintSnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public final String getMessage() {
        StringBuffer sb = new StringBuffer();

        if (errorShowConstraint()) {
            sb.append("Unsatisfiable constraint");
            sb.append('\n');
            sb.append('\t');
            sb.append("general constraint:");
            sb.append('\n');
            sb.append('\t');
            sb.append('\t');
            appendNamedConstraint(sb);
            sb.append('\n');
            sb.append('\t');
            sb.append("in this context:");
            sb.append('\n');
            sb.append('\t');
            sb.append('\t');
            appendActualConstraint(sb);
            sb.append('\n');
            sb.append('\t');
            sb.append("cannot satisfy equation:");
            sb.append('\n');
            sb.append('\t');
            sb.append('\t');
            appendEquation(sb);
            sb.append('\n');
            sb.append('\t');
            sb.append("in environment:");
            sb.append('\n');
            for (LabelLeAssertion assertion : failure.env().labelAssertions()) {
                sb.append('\t');
                sb.append('\t');
                sb.append(assertion.lhs());
                sb.append(" ⊑ ");
                sb.append(assertion.rhs());
                sb.append('\n');
            }
            sb.append('\t');
            sb.append('\t');
            sb.append(failure.env().principalHierarchy());
            sb.append('\n');
            sb.append('\n');
        }

        if (errorShowDefns() && failure instanceof LabelEquation) {
            sb.append("Label Descriptions");
            sb.append('\n');
            sb.append("------------------");
            sb.append('\n');
            for (Map.Entry<String, List<String>> entry : definitions())
                for (String desc : entry.getValue())
                    sb.append(" - " + entry.getKey() + " = " + desc + "\n");
            sb.append('\n');
        }

        if (errorShowTechnicalMsg()) {
            sb.append(failure.constraint().technicalMsg());
        } else if (errorShowDetailMsg()) {
            sb.append(failure.constraint().detailMsg());
        } else {
            sb.append(failure.constraint().msg());
        }

        return sb.toString();
    }

    /**
     * Append the failed named constraint, for example
     *    "caller_pc <= callee_pc"
     * 
     * If the constraint does not have a named component, sb is unmodified.
     */
    protected void appendNamedConstraint(StringBuffer sb) {
        if (!(failure instanceof LabelEquation)) return;

        LabelConstraint lc = ((LabelEquation) failure).labelConstraint();
        if (null == lc.namedLhs()) return;
        if (null == lc.namedRhs()) return;

        sb.append(lc.namedLhs());
        sb.append(lc.kind());
        sb.append(lc.namedRhs());

    }

    /**
     * append the fully instantiated failed constraint, for example
     *    "a;b;c <= b;c"
     * 
     * If the constraint does not have a named component, sb is unmodified.
     */
    protected void appendActualConstraint(StringBuffer sb) {
        sb.append(solver.bounds.applyTo(failure.constraint().lhs));
        sb.append(failure.constraint.kind());
        sb.append(solver.bounds.applyTo(failure.constraint().rhs));
    }

    /**
     * append the failed equation, for example
     *     "a <= b;c"
     * or
     *     "a actsfor b"
     */
    protected void appendEquation(StringBuffer sb) {
        // label equation: lhs ⊑ rhs
        if (failure instanceof LabelEquation) {
            LabelEquation le = (LabelEquation) failure;
            sb.append(le.lhs());
            sb.append(" ⊑ ");
            sb.append(le.rhs());
        }

        // principal equation: lhs ≽ rhs
        else if (failure instanceof PrincipalEquation) {
            PrincipalEquation pe = (PrincipalEquation) failure;
            sb.append(pe.lhs());
            sb.append(" ≽ ");
            sb.append(pe.rhs());
        }

        else throw new InternalCompilerError(failure.position(),
                "unexpected equation type");
    }

    protected Iterable<Map.Entry<String, List<String>>> definitions() {
        if (failure instanceof LabelEquation)
            return ((LabelEquation) failure).labelConstraint()
                    .definitions(solver.bounds).entrySet();
        else return Collections.emptyList();
    }

    ////////////////////////////////////////////////////////////////////////////
    // determine what type of messages to show                                //
    ////////////////////////////////////////////////////////////////////////////

    protected boolean errorShowConstraint() {
        return (errorShowTechnicalMsg() || errorShowDetailMsg());
    }

    protected boolean errorShowTechnicalMsg() {
        return false;
    }

    protected boolean errorShowDetailMsg() {
        return ((JifOptions) Options.global).explainErrors;
    }

    protected boolean errorShowDefns() {
        return (errorShowTechnicalMsg() || errorShowDetailMsg())
                && errorShowConstraint();
    }
}
