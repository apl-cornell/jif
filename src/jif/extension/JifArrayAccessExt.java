package jif.extension;

import java.util.ArrayList;
import java.util.List;

import jif.ast.JifNodeFactory;
import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.ArrayAccess;
import polyglot.ast.ArrayAccessAssign;
import polyglot.ast.Assign;
import polyglot.ast.Expr;
import polyglot.ast.IntLit;
import polyglot.ast.Local;
import polyglot.ast.Node;
import polyglot.types.ArrayType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** The Jif extension of the <code>ArrayAccess</code> node.
 */
public class JifArrayAccessExt extends JifExprExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifArrayAccessExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheckIncrement(LabelChecker lc) throws SemanticException {
        JifNodeFactory nf = (JifNodeFactory) lc.nodeFactory();
        ArrayAccess ae = (ArrayAccess) node();
        Position pos = ae.position();
        ArrayAccessAssign aae = nf.ArrayAccessAssign(pos, ae, Assign.ADD_ASSIGN,
                nf.IntLit(pos, IntLit.INT, 1));

        aae = (ArrayAccessAssign) lc.labelCheck(aae);

        return aae.left();
    }

    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        JifContext A = lc.jifContext();
        JifTypeSystem ts = lc.jifTypeSystem();
        ArrayAccess aie = (ArrayAccess) node();

        List<Type> throwTypes = new ArrayList<Type>(aie.del().throwTypes(ts));

        Expr array = (Expr) lc.context(A).labelCheck(aie.array());
        PathMap Xa = getPathMap(array);

        A = (JifContext) A.pushBlock();
        updateContextForIndex(lc, A, Xa);

        Expr index = (Expr) lc.context(A).labelCheck(aie.index());
        PathMap Xb = getPathMap(index);

        A = (JifContext) A.pop();

        Label La = arrayBaseLabel(array, ts);

        Type npe = ts.NullPointerException();
        Type oob = ts.OutOfBoundsException();

        PathMap X2 = Xa.join(Xb);
        if (!((JifArrayAccessDel) node().del()).arrayIsNeverNull()) {
            // a null pointer exception may be thrown
            checkAndRemoveThrowType(throwTypes, npe);
            X2 = X2.exc(Xa.NV(), npe);
        }
        if (((JifArrayAccessDel) node().del()).outOfBoundsExcThrown()) {
            // an out of bounds exception may be thrown
            checkAndRemoveThrowType(throwTypes, oob);
            X2 = X2.exc(lc.upperBound(Xa.NV(), Xb.NV()), oob);
        }

        PathMap X = X2.NV(lc.upperBound(La, X2.NV()));

        checkThrowTypes(throwTypes);
        return updatePathMap(aie.index(index).array(array), X);
    }

    /**
     * Utility method for updating the context for checking the index
     * expression.
     */
    protected void updateContextForIndex(LabelChecker lc, JifContext A,
            PathMap Xarr) {
        A.setPc(Xarr.N(), lc);
    }

    private Type arrayType(Expr array, JifTypeSystem ts) {
        Type arrayType = array.type();
        if (array instanceof Local) {
            arrayType = ((Local) array).localInstance().type();
        }

        return ts.unlabel(arrayType);
    }

    private Label arrayBaseLabel(Expr array, JifTypeSystem ts) {
        Type arrayType = arrayType(array, ts);
        return ts.labelOfType(((ArrayType) arrayType).base());
    }

}
