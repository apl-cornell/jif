package jif.translate;

import polyglot.ast.Node;
import polyglot.ast.Unary;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class UnaryToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Unary n = (Unary) node();
        return rw.java_nf().Unary(n.position(), n.expr(), n.operator());
    }
}
