package jif.extension;

import java.util.ArrayList;
import java.util.List;

import jif.ast.JifUtil;
import jif.translate.ToJavaExt;
import jif.types.ConstraintMessage;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.LabelConstraint;
import jif.types.NamedLabel;
import jif.types.PathMap;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.ArrayInit;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.SerialVersionUID;

/** The Jif extension of the <code>ArrayInit</code> node.
 */
public class JifArrayInitExt extends JifExprExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifArrayInitExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        ArrayInit init = (ArrayInit) node();

        JifTypeSystem ts = lc.jifTypeSystem();

        JifContext A = lc.jifContext();
        A = (JifContext) init.del().enterScope(A);

        A = (JifContext) A.pushBlock();

        PathMap X = ts.pathMap();
        X = X.N(A.pc());

        List<Expr> l = new ArrayList<Expr>(init.elements().size());

        for (Expr e : init.elements()) {
            e = (Expr) lc.context(A).labelCheck(e);
            l.add(e);

            PathMap Xe = getPathMap(e);
            X = X.N(ts.notTaken()).join(Xe);

            updateContextForNextElem(lc, A, X);
        }

        A = (JifContext) A.pop();

        return updatePathMap(init.elements(l), X);
    }

    /**
     * Utility method for updating the context for checking the next element
     * expression.
     */
    protected void updateContextForNextElem(LabelChecker lc, JifContext A,
            PathMap Xelem) {
        A.setPc(Xelem.N(), lc);
    }

    public void labelCheckElements(LabelChecker lc, Type lhsType)
            throws SemanticException {
        ArrayInit init = (ArrayInit) node();

        // Check if we can assign each individual element.
        Type t = lhsType.toArray().base();
        Label L = null;
        if (lc.typeSystem().isLabeled(t)) {
            L = lc.typeSystem().labelOfType(t);
        }

        for (Expr e : init.elements()) {
            Type s = e.type();

            if (e instanceof ArrayInit) {
                ((JifArrayInitExt) JifUtil.jifExt(e)).labelCheckElements(lc, t);
            }

            SubtypeChecker subtypeChecker = new SubtypeChecker(t, s);
            subtypeChecker.addSubtypeConstraints(lc, e.position());

            if (L != null) {
                // check that the element can be assigned to the base type.
                PathMap Xe = getPathMap(e);
                lc.constrain(
                        new NamedLabel("array_init_elem.nv",
                                "label of successful evaluation of array element "
                                        + e,
                                Xe.NV()),
                        LabelConstraint.LEQ,
                        new NamedLabel("label of array base type", L),
                        lc.context().labelEnv(), e.position(),
                        new ConstraintMessage() {
                            @Override
                            public String msg() {
                                return "Label of the array element not less "
                                        + "restrictive than the label of the array base type.";
                            }

                            @Override
                            public String detailMsg() {
                                return "More information is revealed by the successful "
                                        + "evaluation of the intializing expression "
                                        + "than is allowed to flow to "
                                        + "the array base type.";
                            }
                        });
            }

        }
    }
}
