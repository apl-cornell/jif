package jif.ast;

import polyglot.ext.jl.ast.*;
import jif.types.*;
import jif.visit.*;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.visit.*;
import polyglot.util.*;

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

    public String toString() {
	if (constraint != null) {
	    return constraint.toString();
	}
	else {
	    return "<unknown-constraint>";
	}
    }
}
