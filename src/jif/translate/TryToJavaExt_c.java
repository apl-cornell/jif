package jif.translate;

import polyglot.ast.Node;
import polyglot.ast.Try;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class TryToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Try n = (Try) node();
        return rw.java_nf().Try(n.position(), n.tryBlock(), n.catchBlocks(),
                n.finallyBlock());
    }
}
