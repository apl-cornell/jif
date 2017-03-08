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
     * @param thisQualifier
     *          an Expr representing the translated "this" reference.
     */
    Expr toJava(JifToJavaRewriter rw, Expr thisQualifier)
            throws SemanticException;

    ActsForParam simplify();
}
