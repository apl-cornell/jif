package jif.extension;

import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.ast.Instanceof;
import polyglot.ast.Node;
import polyglot.types.SemanticException;

/** The Jif extension of the <code>Instanceof</code> node. 
 * 
 *  @see polyglot.ast.Instanceof
 */
public class JifInstanceofExt extends Jif_c 
{
    public JifInstanceofExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheck(LabelChecker lc) throws SemanticException {
	Instanceof ioe = (Instanceof) node();
        JifContext A = lc.jifContext();
	A = (JifContext) ioe.enterScope(A);
	Expr e = (Expr) lc.context(A).labelCheck(ioe.expr());
	return X(ioe.expr(e), X(e));
    }
}
