package jif.translate;

import jif.types.principal.ExternalPrincipal;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class ExternalPrincipalToJavaExpr_c extends PrincipalToJavaExpr_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr toJava(Principal principal, JifToJavaRewriter rw,
            Expr thisQualifier) throws SemanticException {
        ExternalPrincipal P = (ExternalPrincipal) principal;
        return rw.qq().parseExpr("jif.principals.%s.getInstance()", P.name());
    }
}
