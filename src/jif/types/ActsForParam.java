package jif.types;

import jif.translate.JifToJavaRewriter;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

/**
 * A class parameter type that can be used in an actsfor constraint.
 */
public interface ActsForParam extends Param {
    ActsForParam subst(LabelSubstitution labelSubst) throws SemanticException;

    /**
     * @param qualifier
     *          an Expr with which to qualify all accesses to label params and
     *          principal params.
     */
    Expr toJava(JifToJavaRewriter rw, Expr qualifier) throws SemanticException;

    ActsForParam simplify();
}
