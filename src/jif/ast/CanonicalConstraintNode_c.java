package jif.ast;

import polyglot.ext.jl.ast.*;
import jif.types.*;
import jif.visit.*;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.visit.*;
import polyglot.util.*;

/** An implementation of the <code>CanonicalConstraint</code>. 
 */
public class CanonicalConstraintNode_c extends ConstraintNode_c implements CanonicalConstraintNode
{
    public CanonicalConstraintNode_c(Position pos, Assertion constraint) {
	super(pos);
	this.constraint = constraint;
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write(constraint.toString());
    }

    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("Cannot translate " + this);
    }
}
