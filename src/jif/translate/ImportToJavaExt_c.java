package jif.translate;

import polyglot.ast.Import;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class ImportToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Import n = (Import) node();
        return rw.java_nf().Import(n.position(), n.kind(), n.name());
    }
}
