package jif.extension;

import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.Throw;
import polyglot.types.SemanticException;

/** Jif extension of the <code>Throw</code> node.
 *  
 *  @see polyglot.ast.Throw
 */
public class JifThrowExt extends JifStmtExt_c
{
    public JifThrowExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheckStmt(LabelChecker lc) throws SemanticException
    {
	Throw ths = (Throw) node();

        JifTypeSystem ts = lc.jifTypeSystem();
	JifContext A = lc.jifContext();
	A = (JifContext) ths.enterScope(A);

	Expr e = (Expr) lc.context(A).labelCheck(ths.expr());
	PathMap Xe = X(e);

	PathMap X = Xe.exc(Xe.NV(), e.type());
	X = X.N(ts.notTaken());
	X = X.NV(ts.notTaken());

	return X(ths, X);
    }
}
