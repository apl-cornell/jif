package jif.extension;

import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.PathMap;
import jif.visit.LabelChecker;
import polyglot.ast.Block;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.Synchronized;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

/** Jif extension of the <code>Synchronized</code> node.
 *  
 *  @see polyglot.ast.Synchronized
 */
public class JifSynchronizedExt extends JifStmtExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifSynchronizedExt(ToJavaExt toJava) {
        super(toJava);
    }

    /** Label check the <tt>synchronized</tt> statement.
     *  Just lets the label checker visit its children. 
     */
    @Override
    public Node labelCheckStmt(LabelChecker lc) throws SemanticException {
        Synchronized ss = (Synchronized) node();

        JifContext A = lc.jifContext();
        A = (JifContext) ss.del().enterScope(A);

        Expr e = (Expr) lc.context(A).labelCheck(ss.expr());
        PathMap Xe = getPathMap(e);

        updateContextForBody(lc, A, Xe);

        Block s = (Block) lc.context(A).labelCheck(ss.body());
        PathMap Xs = getPathMap(s);

        PathMap X = Xe.join(Xs);

        return updatePathMap(ss.body(s), X);
    }

    /**
     * Utility method for updating the context for checking the body.
     *
     * Useful for overriding in projects like Fabric.
     */
    protected void updateContextForBody(LabelChecker lc, JifContext A,
            PathMap Xexpr) {
        A.setPc(Xexpr.N(), lc);
    }
}
