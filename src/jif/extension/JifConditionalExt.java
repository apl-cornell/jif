package jif.extension;

import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.visit.LabelChecker;
import polyglot.ast.Conditional;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;

/** The Jif extension of the <code>Conditional</code> node. 
 * 
 *  @see polyglot.ast.Conditional
 */
public class JifConditionalExt extends JifExprExt
{
    public JifConditionalExt(ToJavaExt toJava) {
        super(toJava);
    }

    SubtypeChecker subtypeChecker = new SubtypeChecker();

    public Node labelCheck(LabelChecker lc) throws SemanticException
    {
        Conditional te = (Conditional) node();

        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();
        A = (JifContext) te.del().enterScope(A);

        Type t1 = te.consequent().type();
        Type t2 = te.alternative().type();
        if (t1.isReference() && t2.isReference()) {
            Type exprType = te.type();
            if (ts.isImplicitCastValid(t1, t2)) {
                subtypeChecker.addSubtypeConstraints(lc, te.position(), exprType, t1);
            }
            if (ts.isImplicitCastValid(t2, t1)) {
                subtypeChecker.addSubtypeConstraints(lc, te.position(), exprType, t2);
            }
        }


        Expr e = (Expr) lc.context(A).labelCheck(te.cond());
        PathMap Xe = getPathMap(e);

        A = (JifContext) A.pushBlock();
        A.setPc(Xe.NV());

        Expr t = (Expr) lc.context(A).labelCheck(te.consequent());
        PathMap Xt = getPathMap(t);

        A = (JifContext) A.pop();

        A = (JifContext) A.pushBlock();
        A.setPc(Xe.NV());

        Expr f = (Expr) lc.context(A).labelCheck(te.alternative());
        PathMap Xf = getPathMap(f);

        A = (JifContext) A.pop();

        PathMap X = Xe.N(ts.notTaken()).join(Xt).join(Xf);

        return updatePathMap(te, X);
    }
}
