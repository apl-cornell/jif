package jif.translate;

import java.io.Serializable;

import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public interface PrincipalToJavaExpr extends Serializable {
    /**
     * @param qualifier
     *          an Expr with which to qualify all accesses to label params and
     *          principal params.
     */
    public Expr toJava(Principal principal, JifToJavaRewriter rw,
            Expr qualifier) throws SemanticException;
}
