package jif.translate;

import polyglot.ast.LocalDecl;
import polyglot.ast.Node;
import polyglot.types.LocalInstance;
import polyglot.types.SemanticException;
import polyglot.visit.NodeVisitor;

public class LocalDeclToJavaExt_c extends ToJavaExt_c {
    protected LocalInstance li = null;
    
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        LocalDecl n = (LocalDecl)this.node();
        this.li = n.localInstance();
        return super.toJavaEnter(rw);
    }

    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        LocalDecl n = (LocalDecl) node();
        n = (LocalDecl) super.toJava(rw);
        return n;
    }
}
