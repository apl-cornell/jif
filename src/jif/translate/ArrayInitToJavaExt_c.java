package jif.translate;

import polyglot.ast.ArrayInit;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class ArrayInitToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        ArrayInit n = (ArrayInit) node();
        return rw.java_nf().ArrayInit(n.position(), n.elements());

    }
}
