package jif.translate;

import polyglot.ast.*;
import polyglot.ast.FieldDecl;
import polyglot.ast.Node;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.SemanticException;
import polyglot.visit.NodeVisitor;

public class FieldDeclToJavaExt_c extends ToJavaExt_c {
    protected FieldInstance fi = null;
    
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        FieldDecl n = (FieldDecl)this.node();
        this.fi = n.fieldInstance();
        return super.toJavaEnter(rw);
    }

    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        FieldDecl n = (FieldDecl) node();
        
        // if it is an instance field with an initializing expression, we need 
        // the initialiazation to the initializer method.
        if (!n.flags().isStatic() && n.init() != null) {
            rw.addInitializer(fi, n.init());
            n = n.init(null);
        }

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
