package jif.translate;

import java.io.Serializable;

import jif.types.ActsForConstraint;
import jif.types.ActsForParam;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public interface ActsForConstraintToJavaExpr extends Serializable {
    <Actor extends ActsForParam, Granter extends ActsForParam> Expr toJava(
            ActsForConstraint<Actor, Granter> actsFor, JifToJavaRewriter rw)
                    throws SemanticException;
}
