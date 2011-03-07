package jif.types;

/**
 * The acts-for constraint.
 */
public interface ActsForConstraint<Actor extends ActsForParam, Granter extends ActsForParam>
        extends Assertion {

    public Actor actor();
    public ActsForConstraint<Actor, Granter> actor(Actor actor);
    
    public Granter granter();
    public ActsForConstraint<Actor, Granter> granter(Granter granter);
    
    public boolean isEquiv();
}
