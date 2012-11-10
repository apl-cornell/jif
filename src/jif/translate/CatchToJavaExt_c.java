package jif.translate;

import polyglot.ast.Catch;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class CatchToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Catch b = (Catch) node();
        return rw.java_nf().Catch(b.position(), b.formal(), b.body());
    }
}
