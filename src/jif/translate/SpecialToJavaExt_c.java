package jif.translate;

import polyglot.ast.Node;
import polyglot.ast.Special;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class SpecialToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Special n = (Special) node();
        return rw.java_nf().Special(n.position(), n.kind(), n.qualifier());
    }
}
