package jif.translate;

import jif.ast.DowngradeExpr;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;
import polyglot.visit.NodeVisitor;

public class DowngradeExprToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw)
            throws SemanticException {
        DowngradeExpr n = (DowngradeExpr) node();
        return rw.bypass(n.bound()).bypass(n.label());
    }

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        DowngradeExpr n = (DowngradeExpr) node();
        return n.expr();
    }
}
