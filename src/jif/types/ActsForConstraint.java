package jif.types;

import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import jif.translate.JifToJavaRewriter;

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
    public Expr toJava(JifToJavaRewriter rw) throws SemanticException;
}
