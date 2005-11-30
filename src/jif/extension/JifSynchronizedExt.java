package jif.extension;

import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.SemanticException;

/** Jif extension of the <code>Synchronized</code> node.
 *  
 *  @see polyglot.ast.Synchronized
 */
public class JifSynchronizedExt extends JifStmtExt_c
{
    public JifSynchronizedExt(ToJavaExt toJava) {
        super(toJava);
    }

    /** Label check the <tt>synchronized</tt> statement.
     *  Just lets the label checker visit its children. 
     */
    public Node labelCheckStmt(LabelChecker lc) throws SemanticException {
	Synchronized ss = (Synchronized) node();

	JifContext A = lc.jifContext();
	A = (JifContext) ss.enterScope(A);

	Expr e = (Expr) lc.context(A).labelCheck(ss.expr());
	PathMap Xe = X(e);

	A.setPc(Xe.N());

	Block s = (Block) lc.context(A).labelCheck(ss.body());
	PathMap Xs = X(s);

	PathMap X = Xe.join(Xs);

	return X(ss, X);
    }
}
