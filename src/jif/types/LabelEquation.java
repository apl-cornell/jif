package jif.types;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jif.types.hierarchy.LabelEnv;
import jif.types.label.JoinLabel;
import jif.types.label.Label;
import jif.types.label.MeetLabel;
import jif.types.label.Variable;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/**
 * Label equation derived from a label constraint. A label equation represents
 * an inequality that must be satisfied, namely <code>lhs <= rhs</code>
 * in the environment <code>env</code>.
 * 
 * 
 * @see jif.types.LabelConstraint
 */
public class LabelEquation extends Equation {
    private Label lhs;
    private Label rhs;

    /**
     * Constructor
     */
    LabelEquation(Label lhs, Label rhs, LabelConstraint constraint) {
        super(constraint);
        this.lhs = lhs;
        this.rhs = rhs.simplify();

        if (lhs instanceof JoinLabel) {
            throw new InternalCompilerError(
                    "LHS of equation must not be a join label.");
        }
        if (rhs instanceof MeetLabel) {
            throw new InternalCompilerError(
                    "LHS of equation must not be a meet label.");
        }
    }

    public Label lhs() {
        return lhs;
    }

    public Label rhs() {
        return rhs;
    }

    @Override
    public LabelEnv env() {
        return constraint().env();
    }

    @Override
    public Position position() {
        return constraint().position();
    }

    public LabelConstraint labelConstraint() {
        return (LabelConstraint) constraint;
    }

    /**
     * Return a <code>List</code> of variable components that occur in either the
     * left or right hand side.
     */
    public List<Variable> variableComponents() {
        List<Variable> l = new LinkedList<Variable>();
        l.addAll(lhs.variableComponents());
        l.addAll(rhs.variableComponents());
        return l;
    }

    @Override
    public Object copy() {
        return new LabelEquation(lhs, rhs, (LabelConstraint) constraint);
    }

    /**
     * Return a <code>Set</code> of variables that occur in either the
     * left or right hand side.
     */
    @Override
    public Set<Variable> variables() {
        Set<Variable> l = new LinkedHashSet<Variable>();
        l.addAll(lhs.variables());
        l.addAll(rhs.variables());
        return l;
    }

    @Override
    public int hashCode() {
        return lhs.hashCode() ^ rhs.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LabelEquation)) {
            return false;
        }

        LabelEquation eqn = (LabelEquation) o;

        if (this.constraint != eqn.constraint) return false;

        return lhs.equals(eqn.lhs) && rhs.equals(eqn.rhs);
    }

    @Override
    public String toString() {
        return lhs.toString() + " <= " + rhs.toString() + " in environment "
                + env() + " (produced from " + labelConstraint().lhsLabel()
                + labelConstraint().kind() + labelConstraint().rhsLabel() + ") "
                + position();
    }

    /**
     * Replace the <code>lhs</code> and <code>rhs</code> with the result of
     * <code>lhs.subst(subst)</code> and <code>rhs.subst(subst)</code>
     * respectively.
     */
    @Override
    public void subst(LabelSubstitution subst) throws SemanticException {
        rhs = rhs.subst(subst);
        lhs = lhs.subst(subst);
    }
}
