package jif.translate;

import polyglot.ast.Node;
import polyglot.ast.SwitchBlock;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class SwitchBlockToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        SwitchBlock n = (SwitchBlock) node();
        return rw.java_nf().SwitchBlock(n.position(), n.statements());
    }
}
