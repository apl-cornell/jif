package jif.translate;

import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public abstract class PrincipalToJavaExpr_c implements PrincipalToJavaExpr {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public abstract Expr toJava(Principal principal, JifToJavaRewriter rw)
            throws SemanticException;
}
