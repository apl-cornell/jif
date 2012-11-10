package jif.translate;

import polyglot.ast.Do;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class DoToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Do n = (Do) node();
        return rw.java_nf().Do(n.position(), n.body(), n.cond());
    }
}
