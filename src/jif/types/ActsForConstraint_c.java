package jif.types;

import jif.translate.ActsForConstraintToJavaExpr;
import jif.translate.JifToJavaRewriter;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.types.TypeObject_c;
import polyglot.types.TypeSystem;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class ActsForConstraint_c<Actor extends ActsForParam, Granter extends ActsForParam>
        extends TypeObject_c implements ActsForConstraint<Actor, Granter> {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected ActsForConstraintToJavaExpr toJava;
    protected Actor actor;
    protected Granter granter;
    protected final boolean isEquiv;

    public ActsForConstraint_c(TypeSystem ts, Position pos, Actor actor,
            Granter granter, boolean isEquiv,
            ActsForConstraintToJavaExpr toJava) {
        super(ts, pos);
        this.actor = actor;
        this.granter = granter;
        this.isEquiv = isEquiv;
        this.toJava = toJava;
    }

    public ActsForConstraint_c(TypeSystem ts, Position pos, Actor actor,
            Granter granter, ActsForConstraintToJavaExpr toJava) {
        this(ts, pos, actor, granter, false, toJava);
    }

    @Override
    public Actor actor() {
        return actor;
    }

    @Override
    public ActsForConstraint<Actor, Granter> actor(Actor actor) {
        ActsForConstraint_c<Actor, Granter> n = copy();
        n.actor = actor;
        return n;
    }

    @Override
    public Granter granter() {
        return granter;
    }

    @Override
    public ActsForConstraint<Actor, Granter> granter(Granter granter) {
        ActsForConstraint_c<Actor, Granter> n = copy();
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
    public ActsForConstraint_c<Actor, Granter> copy() {
        @SuppressWarnings("unchecked")
        ActsForConstraint_c<Actor, Granter> copy =
                (ActsForConstraint_c<Actor, Granter>) super.copy();
        return copy;
    }

    @Override
    public String toString() {
        return actor + " " + (isEquiv ? "equiv" : "actsfor") + " " + granter;
    }

    @Override
    public Expr toJava(JifToJavaRewriter rw) throws SemanticException {
        return toJava.toJava(this, rw);
    }

}
