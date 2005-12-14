package jif.translate;

import jif.types.principal.DisjunctivePrincipal;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public class DisjunctivePrincipalToJavaExpr_c extends PrincipalToJavaExpr_c {
    public Expr toJava(Principal principal, JifToJavaRewriter rw) throws SemanticException {
        DisjunctivePrincipal p = (DisjunctivePrincipal) principal;
        Expr el = rw.principalToJava(p.disjunctLeft());
        Expr er = rw.principalToJava(p.disjunctRight());
        return rw.qq().parseExpr("jif.lang.PrincipalUtil.disjunction(%E, %E)", el, er);
    }
}
