package jif.extension;

import java.util.ArrayList;
import java.util.List;

import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.ast.Instanceof;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.SerialVersionUID;

/** The Jif extension of the <code>Instanceof</code> node.
 *
 *  @see polyglot.ast.Instanceof
 */
public class JifInstanceofExt extends JifExprExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifInstanceofExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        Instanceof ioe = (Instanceof) node();
        JifContext A = lc.jifContext();
        JifTypeSystem ts = lc.typeSystem();

        List<Type> throwTypes = new ArrayList<Type>(ioe.del().throwTypes(ts));
        A = (JifContext) ioe.del().enterScope(A);
        Expr e = (Expr) lc.context(A).labelCheck(ioe.expr());
        PathMap Xe = getPathMap(e);

        // label check the type too, since the type may leak information
        A = (JifContext) A.pushBlock();
        updateContextForType(lc, A, Xe);
        PathMap Xct =
                ts.labelTypeCheckUtil().labelCheckType(ioe.compareType().type(),
                        lc, throwTypes, ioe.compareType().position());
        A = (JifContext) A.pop();
        PathMap X = Xe.N(ts.notTaken()).join(Xct);

        checkThrowTypes(throwTypes);
        return updatePathMap(ioe.expr(e), X);
    }

    /**
     * Utility method for updating the context for checking the type.
     *
     * Useful for overriding in projects like Fabric.
     */
    protected void updateContextForType(LabelChecker lc, JifContext A,
            PathMap Xexpr) {
        A.setPc(Xexpr.N(), lc);
    }
}
