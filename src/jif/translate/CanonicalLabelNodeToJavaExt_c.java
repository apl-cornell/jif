package jif.translate;

import jif.ast.CanonicalLabelNode;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class CanonicalLabelNodeToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        CanonicalLabelNode n = (CanonicalLabelNode) node();
        return rw.labelToJava(n.label());
    }
}
