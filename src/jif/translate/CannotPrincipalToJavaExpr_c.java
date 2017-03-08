package jif.translate;

import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;
import polyglot.util.SerialVersionUID;

public class CannotPrincipalToJavaExpr_c extends PrincipalToJavaExpr_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr toJava(Principal P, JifToJavaRewriter rw, Expr thisQualifier)
            throws SemanticException {
        throw new InternalCompilerError(P.position(),
                "Cannot translate " + P + " to Java.");
    }
}
