package jif.translate;

import java.io.Serializable;

import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public interface PrincipalToJavaExpr extends Serializable {
    /**
     * @param thisQualifier
     *          an Expr representing the translated "this" reference.
     */
    public Expr toJava(Principal principal, JifToJavaRewriter rw,
            Expr thisQualifier) throws SemanticException;
}
