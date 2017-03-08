package jif.extension;

import java.util.ArrayList;
import java.util.List;

import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.visit.LabelChecker;
import polyglot.ast.Binary;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.SerialVersionUID;

/** The Jif extension of the <code>Binary</code> node.
 * 
 *  @see polyglot.ast.Binary_c
 */
public class JifBinaryExt extends JifExprExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifBinaryExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        Binary be = (Binary) node();

        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();

        List<Type> throwTypes = new ArrayList<Type>(be.del().throwTypes(ts));

        A = (JifContext) be.del().enterScope(A);

        Expr left = (Expr) lc.context(A).labelCheck(be.left());
        PathMap Xl = getPathMap(left);

        A = (JifContext) A.pushBlock();

        if (be.operator() == Binary.COND_AND
                || be.operator() == Binary.COND_OR) {
            // if it's a short circuit evaluation, then
            // whether the right is executed or not depends on the _value_
            // of the left sub-expression.
            updateContextForRShort(lc, A, Xl);
        } else {
            // non-short circuit operator, the right sub-expression
            // will always be evaluated, provided the left sub-expression
            // terminated normally.
            updateContextForR(lc, A, Xl);
        }

        Expr right = (Expr) lc.context(A).labelCheck(be.right());
        PathMap Xr = getPathMap(right);

        A = (JifContext) A.pop();

        PathMap X = Xl.N(ts.notTaken()).join(Xr);

        if (((JifBinaryDel) be.del()).throwsArithmeticException()) {
            checkAndRemoveThrowType(throwTypes, ts.ArithmeticException());
            X = X.exc(Xr.NV(), ts.ArithmeticException());
        }

        checkThrowTypes(throwTypes);
        return updatePathMap(be.left(left).right(right), X);
    }

    /**
     * Utility method for updating the context for checking the right expression
     * for a short circuiting operator.
     */
    protected void updateContextForRShort(LabelChecker lc, JifContext A,
            PathMap Xleft) {
        A.setPc(Xleft.NV(), lc);
    }

    /**
     * Utility method for updating the context for checking the right expression
     * for a non-short circuiting operator.
     */
    protected void updateContextForR(LabelChecker lc, JifContext A,
            PathMap Xleft) {
        A.setPc(Xleft.N(), lc);
    }
}
