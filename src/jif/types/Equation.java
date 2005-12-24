package jif.types;

import java.util.LinkedList;
import java.util.List;

import jif.types.hierarchy.LabelEnv;
import jif.types.label.Label;
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
public class Equation
{
    private Label lhs;
    private Label rhs;
    
    /**
     * The <code>LabelConstraint</code> that generated this 
     * <code>Equation</code>.
     */
    private final LabelConstraint constraint; //enclosing constraint

    /**
     * Constructor
     */
    Equation(Label lhs, Label rhs, LabelConstraint constraint) 
    {
	this.lhs = lhs;
	this.rhs = rhs.simplify();
	this.constraint = constraint;

	if (!lhs.isSingleton()) {
	    throw new InternalCompilerError(
		"LHS of equation must be a singleton.");
	}
    }
    
    
    public Label lhs() {return lhs;}
    public Label rhs() {return rhs;}
    public LabelConstraint constraint() { return constraint; }
    public LabelEnv env() {return constraint().env();}
    public Position position() {return constraint().position();}

    /**
     * Return a <code>List</code> of variables that occur in either the 
     * left or right hand side.
     */
    public List variables() {
	List l = new LinkedList();
	l.addAll(lhs.variables());
	l.addAll(rhs.variables());
	return l;
    }

    public int hashCode() { return lhs.hashCode() + rhs.hashCode(); }

    public boolean equals(Object o) {
	if (! (o instanceof Equation)) {
	    return false;
	} 

	Equation eqn = (Equation) o;

	if (! lhs.equals(eqn.lhs) || ! rhs.equals(eqn.rhs)) {
	    return false;
	}

	return constraint == eqn.constraint;
    }

    public String toString() {
	return lhs.toString() + " <= " + rhs.toString() + " in environment " +
                env() + " (produced from " + 
                constraint.lhs() + constraint.kind() + constraint.rhs() + ") " +
                position();
    }
        
    /**
     * Replace the <code>lhs</code> and <code>rhs</code> with the result of 
     * <code>lhs.subst(subst)</code> and <code>rhs.subst(subst)</code> 
     * respectively.
     */
    public void subst(LabelSubstitution subst) throws SemanticException {
        rhs = rhs.subst(subst);
        lhs = lhs.subst(subst);
    }
}
