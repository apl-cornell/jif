package jif.translate;

import jif.ast.CanonicalPrincipalNode;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class CanonicalPrincipalNodeToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        CanonicalPrincipalNode n = (CanonicalPrincipalNode) node();
        return rw.principalToJava(n.principal());
    }
}
