package jif.translate;

import jif.types.principal.ConjunctivePrincipal;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public class ConjunctivePrincipalToJavaExpr_c extends PrincipalToJavaExpr_c {
    public Expr toJava(Principal principal, JifToJavaRewriter rw) throws SemanticException {
        ConjunctivePrincipal p = (ConjunctivePrincipal) principal;
        Expr el = rw.principalToJava(p.conjunctLeft());
        Expr er = rw.principalToJava(p.conjunctRight());
        return rw.qq().parseExpr("jif.lang.PrincipalUtil.conjunction(%E, %E)", el, er);
    }
}
