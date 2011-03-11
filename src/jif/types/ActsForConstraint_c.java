package jif.types;

import polyglot.types.TypeObject_c;
import polyglot.types.TypeSystem;
import polyglot.util.Position;

public class ActsForConstraint_c<Actor extends ActsForParam, Granter extends ActsForParam>
        extends TypeObject_c implements ActsForConstraint<Actor, Granter> {

    protected Actor actor;
    protected Granter granter;
    protected final boolean isEquiv;

    public ActsForConstraint_c(TypeSystem ts, Position pos, Actor actor,
            Granter granter, boolean isEquiv) {
        super(ts, pos);
        this.actor = actor;
        this.granter = granter;
        this.isEquiv = isEquiv;
    }

    public ActsForConstraint_c(TypeSystem ts, Position pos, Actor actor,
            Granter granter) {
        this(ts, pos, actor, granter, false);
    }

    @Override
    public Actor actor() {
        return actor;
    }

    @Override
    public ActsForConstraint<Actor, Granter> actor(Actor actor) {
        @SuppressWarnings("unchecked")
        ActsForConstraint_c<Actor, Granter> n =
                (ActsForConstraint_c<Actor, Granter>) copy();
        n.actor = actor;
        return n;
    }

    @Override
    public Granter granter() {
        return granter;
    }

    @Override
    public ActsForConstraint<Actor, Granter> granter(Granter granter) {
        @SuppressWarnings("unchecked")
        ActsForConstraint_c<Actor, Granter> n =
                (ActsForConstraint_c<Actor, Granter>) copy();
        n.granter = granter;
        return n;
    }

    @Override
    public boolean isEquiv() {
        return isEquiv;
    }

    @Override
    public boolean isCanonical() {
        return actor.isCanonical() && granter.isCanonical();
    }

    @Override
    public String toString() {
        return actor + " " + (isEquiv ? "equiv" : "actsfor") + " " + granter;
    }

}
