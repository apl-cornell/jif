package jif.extension;

import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.visit.LabelChecker;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

/** The Jif extension of the <code>Lit</code> or <code>NewLabel</code> node.
 * 
 *  @see polyglot.ast.Lit
 */
public class JifLiteralExt extends JifExprExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifLiteralExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();
        A = (JifContext) node().del().enterScope(A);

        PathMap X = ts.pathMap();
        X = X.N(A.pc());
        X = X.NV(A.pc());
        return updatePathMap(node(), X);
    }
}
