package jif.translate;

import polyglot.ast.Conditional;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class ConditionalToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Conditional n = (Conditional) node();
        return rw.java_nf().Conditional(n.position(), n.cond(), n.consequent(),
                n.alternative());
    }
}
