package jif.translate;

import polyglot.ast.FieldDecl;
import polyglot.ast.Node;
import polyglot.types.Flags;
import polyglot.types.SemanticException;

public class FieldDeclToJavaExt_c extends ToJavaExt_c {
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        FieldDecl n = (FieldDecl) node();
        n = rw.java_nf().FieldDecl(n.position(), n.flags(), n.type(),
                                   n.name(), n.init());
        if (n.init() == null && n.flags().isFinal()) {
            // Strip "final" to allow translated constructor to assign to it.
	    n = n.flags(n.flags().clear(Flags.FINAL));
        }
        n = n.fieldInstance(null);

        return n;
    }
}
