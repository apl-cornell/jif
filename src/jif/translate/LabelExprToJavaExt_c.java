package jif.translate;

import jif.ast.LabelExpr;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;
import polyglot.visit.NodeVisitor;

public class LabelExprToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw)
            throws SemanticException {
        LabelExpr n = (LabelExpr) node();
        return rw.bypass(n.label());
    }

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        LabelExpr n = (LabelExpr) node();
        return n.visitChild(n.label(), rw);
    }
}
