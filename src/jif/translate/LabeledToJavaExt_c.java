package jif.translate;

import polyglot.ast.Labeled;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class LabeledToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Labeled n = (Labeled) node();
        return rw.java_nf().Labeled(n.position(), n.labelNode(), n.statement());
    }
}
