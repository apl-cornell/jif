package jif.translate;

import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.types.SemanticException;

public class ExprToJavaExt_c extends ToJavaExt_c {
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Expr e = (Expr) node();
        e = e.type(null);
        return exprToJava(rw);
    }

    public Expr exprToJava(JifToJavaRewriter rw) throws SemanticException {
        return (Expr) super.toJava(rw);
    }
}
