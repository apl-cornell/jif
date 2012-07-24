package jif.translate;


import jif.types.JifTypeSystem;
import jif.types.principal.ConjunctivePrincipal;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;

public class ConjunctivePrincipalToJavaExpr_c extends PrincipalToJavaExpr_c {
    @Override
    public Expr toJava(Principal principal, JifToJavaRewriter rw) throws SemanticException {
        JifTypeSystem ts = rw.jif_ts();
        Expr e = null;
        ConjunctivePrincipal cp = (ConjunctivePrincipal) principal;
        for (Principal p : cp.conjuncts()) {
            Expr pe = rw.principalToJava(p);
            if (e == null) {
                e = pe;
            }
            else {
                e = rw.qq().parseExpr(ts.PrincipalUtilClassName() + ".conjunction(%E, %E)", pe, e);
            }
        }
        return e;
    }
}
