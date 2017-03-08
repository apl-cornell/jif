package jif.translate;

import jif.types.JifTypeSystem;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class BottomPrincipalToJavaExpr_c extends PrincipalToJavaExpr_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr toJava(Principal principal, JifToJavaRewriter rw,
            Expr thisQualifier) throws SemanticException {
        JifTypeSystem ts = rw.jif_ts();
        return rw.qq()
                .parseExpr(ts.PrincipalUtilClassName() + ".bottomPrincipal()");
    }
}
