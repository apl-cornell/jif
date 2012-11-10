package jif.translate;

import polyglot.ast.If;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class IfToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        If n = (If) node();
        return rw.java_nf().If(n.position(), n.cond(), n.consequent(),
                n.alternative());
    }
}
