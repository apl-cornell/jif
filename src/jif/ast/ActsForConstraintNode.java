package jif.ast;

import jif.types.ActsForConstraint;
import jif.types.ActsForParam;

/**
 * An immutable representation of the Jif <code>ActsFor constraint</code>.
 * <p>
 * Grammar: <tt>actsFor (actor, granter)</tt>
 * </p>
 * <p>
 * The <tt>ActsFor constraint</tt> only appears in the <tt>where</tt> clause of
 * a procedure header.
 * </p>
 */
public interface ActsForConstraintNode<Actor extends ActsForParam, Granter extends ActsForParam>
        extends ConstraintNode<ActsForConstraint<Actor, Granter>> {
    /** Gets the actor. */
    ActsForParamNode<Actor> actor();

    /** Returns a copy of this node with the actor updated. */
    ActsForConstraintNode<Actor, Granter> actor(ActsForParamNode<Actor> actor);

    /** Gets the granter. */
    ActsForParamNode<Granter> granter();

    /** Returns a copy of this node with the granter updated. */
    ActsForConstraintNode<Actor, Granter> granter(
            ActsForParamNode<Granter> granter);
}
