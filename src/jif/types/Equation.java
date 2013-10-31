package jif.types;

import java.util.Set;

import jif.types.hierarchy.LabelEnv;
import jif.types.label.Variable;
import polyglot.types.SemanticException;
import polyglot.util.Position;

/** 
 * Label equation derived from a label constraint. A label equation represents
 * an inequality that must be satisfied, namely <code>lhs <= rhs</code>
 * in the environment <code>env</code>.
 * 
 * 
 * @see jif.types.LabelConstraint 
 */
public abstract class Equation {
    /**
     * @param constraint
     *        the constraint from which this equation was derived.
     */
    protected Equation(Constraint constraint) {
        this.constraint = constraint;
    }

    /** The constraint from which this equation was derived. */
    protected final Constraint constraint;

    public LabelEnv env() {
        return constraint().env();
    }

    public Position position() {
        return constraint().position();
    }

    /** The constraint from which this equation was derived. */
    public Constraint constraint() {
        return constraint;
    }

    /**
     * Return a <code>Set</code> of variables that occur in either the 
     * left or right hand side.
     */
    public abstract Set<Variable> variables();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract String toString();

    /**
     * Replace the <code>lhs</code> and <code>rhs</code> with the result of 
     * <code>lhs.subst(subst)</code> and <code>rhs.subst(subst)</code> 
     * respectively.
     */
    public abstract void subst(LabelSubstitution subst)
            throws SemanticException;

    public abstract Object copy();
}
