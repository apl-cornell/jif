package jif.ast;

import polyglot.ext.jl.ast.*;
import jif.types.*;
import jif.visit.*;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.visit.*;
import polyglot.util.*;

/** An implementation of the <tt>ActsForConstraintNode</tt> interface. */
public class ActsForConstraintNode_c extends ConstraintNode_c implements ActsForConstraintNode
{
    protected PrincipalNode actor;
    protected PrincipalNode granter;

    public ActsForConstraintNode_c(Position pos, PrincipalNode actor, PrincipalNode granter) {
	super(pos);
	this.actor = actor;
	this.granter = granter;
    }

    /** Gets the actor principal. */
    public PrincipalNode actor() {
	return this.actor;
    }

    /** Returns a copy of this node with the actor updated. */
    public ActsForConstraintNode actor(PrincipalNode actor) {
	ActsForConstraintNode_c n = (ActsForConstraintNode_c) copy();
	n.actor = actor;
	if (constraint() != null) {
	    n.constraint = ((ActsForConstraint)constraint()).actor(actor.principal());
	}
	return n;
    }

    /** Gets the granter principal. */
    public PrincipalNode granter() {
	return this.granter;
    }

    /** Returns a copy of this node with the granter updated. */
    public ActsForConstraintNode granter(PrincipalNode granter) {
	ActsForConstraintNode_c n = (ActsForConstraintNode_c) copy();
	n.granter = granter;
	if (constraint() != null) {
	    n.constraint = ((ActsForConstraint)constraint).granter(granter.principal());
	}
	return n;
    }

    /** Reconstructs this node. */
    protected ActsForConstraintNode_c reconstruct(PrincipalNode actor, PrincipalNode granter) {
	if (actor != this.actor || granter != this.granter) {
	    ActsForConstraintNode_c n = (ActsForConstraintNode_c) copy();
	    return (ActsForConstraintNode_c) n.actor(actor).granter(granter);
	}

	return this;
    }

    /** Visits the children of this node. */
    public Node visitChildren(NodeVisitor v) {
	PrincipalNode actor = (PrincipalNode) visitChild(this.actor, v);
	PrincipalNode granter = (PrincipalNode) visitChild(this.granter, v);
	return reconstruct(actor, granter);
    }

    /** Builds the type of this node. */
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
	if (constraint() == null) {
            JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
            return constraint(ts.actsForConstraint(position(),
                                                  actor.principal(),
                                                  granter.principal()));
        }

        return this;
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("actsFor(");
        print(actor, w, tr);
        w.write(", ");
        print(granter, w, tr);
        w.write(")");
    }

    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("Cannot translate " + this);
    }
}
