package jif.extension;

import java.util.List;

import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.SemanticException;
import polyglot.util.Position;

/** The Jif extension of the <code>Cast</code> node.
 *
 *  @see polyglot.ast.Cast
 */
public class JifCastExt extends Jif_c
{
    public JifCastExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheck(LabelChecker lc) throws SemanticException {
        Cast c = (Cast) node();

        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.context();
        A = (JifContext) c.enterScope(A);

        List throwTypes = c.del().throwTypes(ts);
        Position pos = c.position();

        Expr e = (Expr) lc.context(A).labelCheck(c.expr());
        PathMap Xe = X(e);

	// label check the type too, since the type may leak information
	A = (JifContext) A.pushBlock();
	A.setPc(Xe.N());
	PathMap Xct = LabelTypeCheckUtil.labelCheckType(c.castType().type(), lc, throwTypes, pos);
        A = (JifContext) A.pop();
	PathMap X = Xe.N(ts.notTaken()).join(Xct);

	// the cast may throw a class cast exception.
        if (c.expr().type().isReference()) {
            checkAndRemoveThrowType(throwTypes, ts.ClassCastException());
            X = X.exc(X.NV(), ts.ClassCastException());
        }

        checkThrowTypes(throwTypes);
        return X(c.expr(e), X);
    }
}
