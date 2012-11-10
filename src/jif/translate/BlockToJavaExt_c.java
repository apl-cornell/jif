package jif.translate;

import polyglot.ast.Block;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class BlockToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Block b = (Block) node();
        return rw.java_nf().Block(b.position(), b.statements());
    }
}
