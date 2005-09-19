package jif.ast;

import jif.types.Assertion;
import polyglot.ext.jl.ast.Node_c;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.visit.ExceptionChecker;
import polyglot.visit.NodeVisitor;

/** An implementation of the <code>ConstraintNode</code> interface. 
 */
public class ConstraintNode_c extends Node_c implements ConstraintNode
{
    protected Assertion constraint;

    public ConstraintNode_c(Position pos) {
	super(pos);
    }

    public boolean isDisambiguated() {
        return constraint != null && constraint.isCanonical() && super.isDisambiguated();
    }

    public Assertion constraint() {
	return constraint;
    }

    public ConstraintNode constraint(Assertion constraint) {
	ConstraintNode_c n = (ConstraintNode_c) copy();
	n.constraint = constraint;
	return n;
    }

    /**
     * Bypass all children when peforming an exception check. Constraints
     * aren't examined at runtime.
     */
    public NodeVisitor exceptionCheckEnter(ExceptionChecker ec)
    throws SemanticException
    {
        ec = (ExceptionChecker) super.exceptionCheckEnter(ec);
        return ec.bypassChildren(this);
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
