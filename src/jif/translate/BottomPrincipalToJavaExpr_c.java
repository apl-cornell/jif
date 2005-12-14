package jif.translate;

import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public class BottomPrincipalToJavaExpr_c extends PrincipalToJavaExpr_c {
    public Expr toJava(Principal principal, JifToJavaRewriter rw) throws SemanticException {
        return rw.qq().parseExpr("jif.lang.PrincipalUtil.bottomPrincipal()"); 
    }
}
