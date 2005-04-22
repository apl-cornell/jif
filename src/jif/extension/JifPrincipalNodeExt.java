package jif.extension;

import jif.ast.Jif_c;
import jif.ast.PrincipalNode;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.types.label.AccessPath;
import jif.types.label.AccessPathLocal;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Node;
import polyglot.types.SemanticException;

public class JifPrincipalNodeExt extends Jif_c {
    public JifPrincipalNodeExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheck(LabelChecker lc) throws SemanticException {
        PrincipalNode pn = (PrincipalNode)node();

        JifContext A = lc.jifContext();
        A = (JifContext)pn.enterScope(A);
        JifTypeSystem ts = lc.jifTypeSystem();

        Principal p = pn.principal();
        // make sure the principal is runtime representable
        if (!p.isRuntimeRepresentable()) {
            throw new SemanticException(
                    "A principal used in an expression must be representable at runtime. Principal parameters are not represented at runtime.",
                    pn.position());
        }

        PathMap X1 = p.labelCheck(A);
        A = (JifContext)A.pop();

        return X(pn, X1);
    }

}