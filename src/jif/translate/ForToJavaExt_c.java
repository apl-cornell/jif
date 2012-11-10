package jif.translate;

import polyglot.ast.For;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class ForToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        For n = (For) node();
        return rw.java_nf().For(n.position(), n.inits(), n.cond(), n.iters(),
                n.body());
    }
}
