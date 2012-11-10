package jif.translate;

import polyglot.ast.Node;
import polyglot.ast.While;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class WhileToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        While n = (While) node();
        return rw.java_nf().While(n.position(), n.cond(), n.body());
    }
}
