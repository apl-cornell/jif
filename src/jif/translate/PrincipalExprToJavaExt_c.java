package jif.translate;

import jif.ast.PrincipalExpr;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;
import polyglot.visit.NodeVisitor;

public class PrincipalExprToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw)
            throws SemanticException {
        PrincipalExpr n = (PrincipalExpr) node();
        return rw.bypass(n.principal());
    }

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        PrincipalExpr n = (PrincipalExpr) node();
        return n.visitChild(n.principal(), rw);
    }
}
