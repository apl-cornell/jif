package jif.ast;

import jif.types.Assertion;
import polyglot.ext.jl.ast.Node_c;
import polyglot.util.Position;

/** An implementation of the <code>ConstraintNode</code> interface. 
 */
public class ConstraintNode_c extends Node_c implements ConstraintNode
{
    protected Assertion constraint;

    public ConstraintNode_c(Position pos) {
	super(pos);
    }

    public Assertion constraint() {
	return constraint;
    }

    public ConstraintNode constraint(Assertion constraint) {
	ConstraintNode_c n = (ConstraintNode_c) copy();
	n.constraint = constraint;
	return n;
    }

    public String toString() {
	if (constraint != null) {
	    return constraint.toString();
	}
	else {
	    return "<unknown-constraint>";
	}
    }
}
