package jif.translate;

import polyglot.ast.LocalDecl;
import polyglot.ast.Node;
import polyglot.types.SemanticException;

public class LocalDeclToJavaExt_c extends ToJavaExt_c {
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        LocalDecl n = (LocalDecl) node();
        n = (LocalDecl) super.toJava(rw);
        return n;
    }
}
