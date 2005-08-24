package jif.translate;

import polyglot.ast.*;
import polyglot.ast.FieldDecl;
import polyglot.ast.Node;
import polyglot.types.Flags;
import polyglot.types.SemanticException;

public class FieldDeclToJavaExt_c extends ToJavaExt_c {
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        FieldDecl n = (FieldDecl) node();
        
        // if it is an instance field with an initializing expression, we need 
        // the initialiazation to the initializer method.
        if (!n.flags().isStatic() && n.init() != null) {
            Expr target = rw.qq().parseExpr("this." + n.name());
            Assign a = rw.nodeFactory().Assign(n.position(), target, Assign.ASSIGN, n.init());
            n = n.init(null);
            rw.addInitializer(rw.nodeFactory().Eval(n.position(), a));
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
