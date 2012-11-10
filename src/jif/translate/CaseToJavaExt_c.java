package jif.translate;

import polyglot.ast.Case;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class CaseToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Case n = (Case) node();
        long value = n.value();
        n = rw.java_nf().Case(n.position(), n.expr());
        n = n.value(value);
        return n;
    }
}
