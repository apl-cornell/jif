package jif.translate;

import polyglot.ast.Node;
import polyglot.ast.Synchronized;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class SynchronizedToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Synchronized n = (Synchronized) node();
        return rw.java_nf().Synchronized(n.position(), n.expr(), n.body());
    }
}
