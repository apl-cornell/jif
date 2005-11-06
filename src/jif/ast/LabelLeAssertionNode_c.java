package jif.ast;

import jif.types.JifTypeSystem;
import jif.types.LabelLeAssertion;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.*;
import polyglot.visit.*;

/** An implementation of the <tt>ActsForConstraintNode</tt> interface. */
public class LabelLeAssertionNode_c extends ConstraintNode_c implements LabelLeAssertionNode
{
    protected LabelNode lhs;
    protected LabelNode rhs;
    protected final boolean isEquiv;

    public LabelLeAssertionNode_c(Position pos, LabelNode lhs, LabelNode rhs, boolean isEquiv) {
	super(pos);
	this.lhs = lhs;
	this.rhs = rhs;
	this.isEquiv = isEquiv;
    }

    /** Gets the lhs label node. */
    public LabelNode lhs() {
	return this.lhs;
    }

    /** Returns a copy of this node with the lhs updated. */
    public LabelLeAssertionNode lhs(LabelNode lhs) {
	LabelLeAssertionNode_c n = (LabelLeAssertionNode_c) copy();
	n.lhs = lhs;
	if (constraint() != null) {
	    n.constraint = ((LabelLeAssertion)constraint()).lhs(lhs.label());
	}
	return n;
    }

    /** Gets the rhs principal. */
    public LabelNode rhs() {
	return this.rhs;
    }

    /** Returns a copy of this node with the rhs updated. */
    public LabelLeAssertionNode rhs(LabelNode rhs) {
	LabelLeAssertionNode_c n = (LabelLeAssertionNode_c) copy();
	n.rhs = rhs;
	if (constraint() != null) {
	    n.constraint = ((LabelLeAssertion)constraint).rhs(rhs.label());
	}
	return n;
    }

    /** Reconstructs this node. */
    protected LabelLeAssertionNode_c reconstruct(LabelNode lhs, LabelNode rhs) {
	if (lhs != this.lhs || rhs != this.rhs) {
	    LabelLeAssertionNode_c n = (LabelLeAssertionNode_c) copy();
	    return (LabelLeAssertionNode_c) n.lhs(lhs).rhs(rhs);
	}

	return this;
    }

    /** Visits the children of this node. */
    public Node visitChildren(NodeVisitor v) {
	LabelNode lhs = (LabelNode) visitChild(this.lhs, v);
	LabelNode rhs = (LabelNode) visitChild(this.rhs, v);
	return reconstruct(lhs, rhs);
    }

    /** Builds the type of this node. */
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
	if (constraint() == null) {
            JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
            return constraint(ts.labelLeAssertion(position(),
                                                  lhs.label(),
                                                  rhs.label()));
        }

        return this;
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        print(lhs, w, tr);
        w.write(" ");
        w.write(isEquiv?"equiv":"<=");
        w.write(" ");
        print(rhs, w, tr);
    }

    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("Cannot translate " + this);
    }
}
