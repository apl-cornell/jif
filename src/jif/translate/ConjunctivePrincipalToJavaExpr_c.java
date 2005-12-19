package jif.translate;

import java.util.Iterator;

import jif.types.principal.ConjunctivePrincipal;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public class ConjunctivePrincipalToJavaExpr_c extends PrincipalToJavaExpr_c {
    public Expr toJava(Principal principal, JifToJavaRewriter rw) throws SemanticException {
        Expr e = null;
        ConjunctivePrincipal cp = (ConjunctivePrincipal) principal;
        for (Iterator iter = cp.conjuncts().iterator(); iter.hasNext();) {
            Principal p = (Principal)iter.next();
            Expr pe = rw.principalToJava(p);
            if (e == null) {
                e = pe;
            }
            else {
                e = rw.qq().parseExpr("jif.lang.PrincipalUtil.conjunction(%E, %E)", pe, e);
            }
        }
        return e;
    }
}
