/* begin-new */

package jif.translate;

import jif.ast.ReclassifyExpr;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;
import polyglot.visit.NodeVisitor;

public class ReclassifyExprToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw)
            throws SemanticException {
        ReclassifyExpr n = (ReclassifyExpr) node();
        //probably this is wrong, because it may need to bypass the name (which is not a node)
        return rw;
    }

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        ReclassifyExpr n = (ReclassifyExpr) node();
        return n.expr();
    }

}

/* end-new */
