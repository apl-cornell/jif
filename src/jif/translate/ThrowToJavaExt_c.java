package jif.translate;

import polyglot.ast.Node;
import polyglot.ast.Throw;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class ThrowToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Throw n = (Throw) node();
        return rw.java_nf().Throw(n.position(), n.expr());
    }
}
