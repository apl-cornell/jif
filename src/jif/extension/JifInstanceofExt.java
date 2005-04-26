package jif.extension;

import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.JifContext;
import jif.types.Path;
import jif.types.PathMap;
import jif.visit.LabelChecker;
import polyglot.ast.*;
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
        JifTypeSystem ts = lc.typeSystem();
	A = (JifContext) ioe.enterScope(A);
	Expr e = (Expr) lc.context(A).labelCheck(ioe.expr());
	PathMap Xe = X(e);

	// label check the type too, since the type may leak information
	A = (JifContext) A.pushBlock();
	A.setPc(Xe.N());
	PathMap Xct = LabelTypeCheckUtil.labelCheckType(ioe.compareType().type(), lc, ioe.compareType().position());
        A = (JifContext) A.pop();
	PathMap X = Xe.N(ts.notTaken()).join(Xct);

	return X(ioe.expr(e), X);
    }
}
