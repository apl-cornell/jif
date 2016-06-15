package jif.translate;

import jif.types.JifTypeSystem;
import jif.types.principal.ConjunctivePrincipal;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class ConjunctivePrincipalToJavaExpr_c extends PrincipalToJavaExpr_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr toJava(Principal principal, JifToJavaRewriter rw,
            Expr thisQualifier) throws SemanticException {
        JifTypeSystem ts = rw.jif_ts();
        Expr e = null;
        ConjunctivePrincipal cp = (ConjunctivePrincipal) principal;
        for (Principal p : cp.conjuncts()) {
            Expr pe = rw.principalToJava(p, thisQualifier);
            if (e == null) {
                e = pe;
            } else {
                e = rw.qq().parseExpr(
                        ts.PrincipalUtilClassName() + ".conjunction(%E, %E)",
                        pe, e);
            }
        }
        return e;
    }
}
