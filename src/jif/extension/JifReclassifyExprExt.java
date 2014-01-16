/* new-begin */

package jif.extension;

import jif.ast.ReclassifyExpr;
import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.PathMap;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.ast.Ext;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class JifReclassifyExprExt extends JifExprExt implements Ext {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifReclassifyExprExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        final ReclassifyExpr d = (ReclassifyExpr) node();

        JifContext A = lc.jifContext();
        A = (JifContext) d.del().enterScope(A);

        // get the label on e without pc environment
        Expr e = (Expr) lc.context(A).labelCheck(d.expr());
        PathMap Xe = getPathMap(e);

        // The pc at the point of declassification is dependent on the expression to declassify
        // terminating normally.
        A = (JifContext) A.pushBlock();
        A.setPc(Xe.N(), lc);
        lc = lc.context(A);

        Label CHANGEME = lc.typeSystem().labelOfType(e.type());

        PathMap X = Xe.NV(lc.upperBound(A.pc(), CHANGEME));

        return updatePathMap(d.expr(e), X);
    }
}

/* new-end */
