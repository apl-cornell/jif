package jif.extension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jif.ast.JifUtil;
import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.ArrayInit;
import polyglot.ast.Expr;
import polyglot.ast.NewArray;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;

/** The Jif extension of the <code>NewArray</code> node. 
 * 
 *  @see polyglot.ast.NewArray
 */
public class JifNewArrayExt extends JifExprExt 
{
    public JifNewArrayExt(ToJavaExt toJava) {
        super(toJava);
    }

    SubtypeChecker subtypeChecker = new SubtypeChecker();

    public Node labelCheck(LabelChecker lc) throws SemanticException
    {
        JifTypeSystem ts = lc.jifTypeSystem();

        NewArray nae = (NewArray) node();
        List throwTypes = new ArrayList(nae.del().throwTypes(ts));

        JifContext A = lc.jifContext();
        A = (JifContext) nae.del().enterScope(A);

        A = (JifContext) A.pushBlock();

        PathMap Xs = ts.pathMap();
        Xs = Xs.N(A.pc());

        List dims = new LinkedList();

        Label dimsNV = ts.bottomLabel();
        for (Iterator iter = nae.dims().iterator(); iter.hasNext(); ) {
            Expr e = (Expr) iter.next(); 
            e = (Expr) lc.context(A).labelCheck(e);
            dims.add(e);

            PathMap Xe = getPathMap(e);
            Xs = Xs.N(ts.notTaken()).join(Xe);

            A.setPc(Xs.N());
            dimsNV = ts.join(dimsNV, Xe.NV());
        }

        ArrayInit init = null;

        if (nae.init() != null) {
            init = (ArrayInit) lc.context(A).labelCheck(nae.init());
            ((JifArrayInitExt)(JifUtil.jifExt(init))).labelCheckElements(lc, nae.type()); 
            PathMap Xinit = getPathMap(init);
            Xs = Xs.N(ts.notTaken()).join(Xinit);
        }

        if (!((JifNewArrayDel)node().del()).noNegArraySizeExcThrown()) {
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
}
