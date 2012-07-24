package jif.extension;

import java.util.List;

import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.visit.LabelChecker;
import polyglot.ast.Cast;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;

/** The Jif extension of the <code>Cast</code> node.
 *
 *  @see polyglot.ast.Cast
 */
public class JifCastExt extends JifExprExt
{
    public JifCastExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        Cast c = (Cast) node();

        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.context();
        A = (JifContext) c.del().enterScope(A);

        List<Type> throwTypes = c.del().throwTypes(ts);
        Position pos = c.position();

        Expr e = (Expr) lc.context(A).labelCheck(c.expr());
        PathMap Xe = getPathMap(e);

        // label check the type too, since the type may leak information
        A = (JifContext) A.pushBlock();
        A.setPc(Xe.N(), lc);
        PathMap Xct = ts.labelTypeCheckUtil().labelCheckType(c.castType().type(), lc, throwTypes, pos);
        A = (JifContext) A.pop();
        PathMap X = Xe.N(ts.notTaken()).join(Xct);

        // the cast may throw a class cast exception.
        if (c.expr().type().isReference() && ((JifCastDel)c.del()).throwsClassCastException()) {
            checkAndRemoveThrowType(throwTypes, ts.ClassCastException());
            X = X.exc(X.NV(), ts.ClassCastException());
        }

        checkThrowTypes(throwTypes);
        return updatePathMap(c.expr(e), X);
    }
}
