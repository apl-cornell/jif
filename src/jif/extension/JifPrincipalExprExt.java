package jif.extension;

import java.util.ArrayList;
import java.util.List;

import jif.ast.PrincipalExpr;
import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.types.SemanticDetailedException;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.SerialVersionUID;

public class JifPrincipalExprExt extends JifExprExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifPrincipalExprExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        PrincipalExpr pe = (PrincipalExpr) node();
        Principal p = pe.principal().principal();
        JifContext A = lc.jifContext();
        JifTypeSystem ts = lc.jifTypeSystem();

        List<Type> throwTypes = new ArrayList<Type>(pe.del().throwTypes(ts));
        // make sure the principal is runtime representable
        if (!p.isRuntimeRepresentable()) {
            throw new SemanticDetailedException(
                    "Principal expression not representable at runtime.",
                    "A principal expression must be representable at runtime.",
                    pe.position());
        }

        A = (JifContext) pe.del().enterScope(A);

        PathMap X1 = p.labelCheck(A, lc);
        throwTypes.removeAll(p.throwTypes(ts));

        A = (JifContext) A.pop();
        checkThrowTypes(throwTypes);
        return updatePathMap(pe, X1);
    }

}
