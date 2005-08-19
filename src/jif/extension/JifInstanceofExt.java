package jif.extension;

import java.util.ArrayList;
import java.util.List;

import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.visit.LabelChecker;
import polyglot.ast.*;
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

        List throwTypes = new ArrayList(ioe.del().throwTypes(ts));
        A = (JifContext) ioe.enterScope(A);
	Expr e = (Expr) lc.context(A).labelCheck(ioe.expr());
	PathMap Xe = X(e);

	// label check the type too, since the type may leak information
	A = (JifContext) A.pushBlock();
	A.setPc(Xe.N());
	PathMap Xct = LabelTypeCheckUtil.labelCheckType(ioe.compareType().type(), lc, throwTypes, ioe.compareType().position());
        A = (JifContext) A.pop();
	PathMap X = Xe.N(ts.notTaken()).join(Xct);

	checkThrowTypes(throwTypes);
	return X(ioe.expr(e), X);
    }
}
