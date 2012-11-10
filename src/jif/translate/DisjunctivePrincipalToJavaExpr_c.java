package jif.translate;

import jif.types.JifTypeSystem;
import jif.types.principal.DisjunctivePrincipal;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class DisjunctivePrincipalToJavaExpr_c extends PrincipalToJavaExpr_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr toJava(Principal principal, JifToJavaRewriter rw)
            throws SemanticException {
        JifTypeSystem ts = rw.jif_ts();
        Expr e = null;
        DisjunctivePrincipal dp = (DisjunctivePrincipal) principal;
        for (Principal p : dp.disjuncts()) {
            Expr pe = rw.principalToJava(p);
            if (e == null) {
                e = pe;
            } else {
                e =
                        rw.qq().parseExpr(
                                ts.PrincipalUtilClassName()
                                        + ".disjunction(%E, %E)", pe, e);
            }
        }
        return e;
    }
}
