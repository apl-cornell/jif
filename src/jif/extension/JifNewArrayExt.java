/**
 * Check the dim expressions to see if any of them can cause
 * a NegativeArraySizeException to be thrown
 */
package jif.extension;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jif.ast.JifUtil;
import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.types.label.Label;
import jif.visit.IntegerBoundsChecker;
import jif.visit.LabelChecker;
import polyglot.ast.ArrayInit;
import polyglot.ast.Expr;
import polyglot.ast.NewArray;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.SerialVersionUID;

/** The Jif extension of the <code>NewArray</code> node.
 * 
 *  @see polyglot.ast.NewArray
 */
public class JifNewArrayExt extends JifExprExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifNewArrayExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        JifTypeSystem ts = lc.jifTypeSystem();

        NewArray nae = (NewArray) node();
        List<Type> throwTypes = new ArrayList<Type>(nae.del().throwTypes(ts));

        JifContext A = lc.jifContext();
        A = (JifContext) nae.del().enterScope(A);

        A = (JifContext) A.pushBlock();

        PathMap Xs = ts.pathMap();
        Xs = Xs.N(A.pc());

        List<Expr> dims = new LinkedList<Expr>();

        Label dimsNV = ts.bottomLabel();
        for (Expr e : nae.dims()) {
            e = (Expr) lc.context(A).labelCheck(e);
            dims.add(e);

            PathMap Xe = getPathMap(e);
            Xs = Xs.N(ts.notTaken()).join(Xe);

            updateContextForDims(lc, A, Xs);
            dimsNV = ts.join(dimsNV, Xe.NV());
        }

        ArrayInit init = null;

        if (nae.init() != null) {
            init = (ArrayInit) lc.context(A).labelCheck(nae.init());
            ((JifArrayInitExt) (JifUtil.jifExt(init))).labelCheckElements(lc,
                    nae.type());
            PathMap Xinit = getPathMap(init);
            Xs = Xs.N(ts.notTaken()).join(Xinit);
        }

        if (!((JifNewArrayDel) node().del()).noNegArraySizeExcThrown()) {
            // a NegativeArraySizeExcepiton may be thrown, depending
            // on the value of the dimensions.
            Type nase = ts.typeForName("java.lang.NegativeArraySizeException");
            checkAndRemoveThrowType(throwTypes, nase);
            Xs = Xs.exc(dimsNV, nase);
        }

        A = (JifContext) A.pop();

        checkThrowTypes(throwTypes);

        return updatePathMap(nae.dims(dims).init(init), Xs);
    }

    @Override
    public void integerBoundsCalculated() {
        super.integerBoundsCalculated();
        boolean noNegArraySizeExcThrown = noNegArraySizeExcThrown();
        if (noNegArraySizeExcThrown) {
            JifNewArrayDel del = (JifNewArrayDel) this.node().del();
            del.setNoNegArraySizeExcThrown();
        }
    }

    private boolean noNegArraySizeExcThrown() {
        NewArray na = (NewArray) node();
        List<Expr> dims = na.dims();
        if (dims == null) return true;
        for (Expr d : dims) {
            JifExprExt ext = (JifExprExt) JifUtil.jifExt(d);

            IntegerBoundsChecker.Interval bounds = ext.getNumericBounds();
            // if bound is not null, then bound < d
            if (bounds == null || bounds.getLower() < 0) {
                // the value of d may be less than 0, and so
                // a NegativeArraySizeException may be thrown

//                System.err.println("Bound for " +  d + " is " + bound);
                return false;
            }
        }
        return true;
    }

    /**
     * Utility method for updating the context for checking the dims.
     *
     * Useful for overriding in projects like Fabric.
     */
    protected void updateContextForDims(LabelChecker lc, JifContext A,
            PathMap Xprev) {
        A.setPc(Xprev.N(), lc);
    }
}
