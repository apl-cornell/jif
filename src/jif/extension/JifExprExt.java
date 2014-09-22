package jif.extension;

import jif.ast.JifExt_c;
import jif.translate.ToJavaExt;
import jif.visit.IntegerBoundsChecker;
import polyglot.ast.Expr;
import polyglot.ast.ExprOps;
import polyglot.ast.Lang;
import polyglot.util.SerialVersionUID;

/** The Jif extension for all <code>Expr</code> nodes.
 */
public class JifExprExt extends JifExt_c implements ExprOps {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifExprExt(ToJavaExt toJava) {
        super(toJava);
    }

    /**
     * If the expression is of integral type, and
     * we can determine a strict lower bound, then this
     * value is set by the {@link IntegerBoundsChecker}.
     */
    private IntegerBoundsChecker.Interval numericBounds = null;

    public void setNumericBounds(IntegerBoundsChecker.Interval numericBounds) {
        this.numericBounds = numericBounds;
    }

    public IntegerBoundsChecker.Interval getNumericBounds() {
        return this.numericBounds;
    }

    @Override
    public boolean constantValueSet(Lang lang) {
        return superLang().constantValueSet((Expr) node(), lang);
    }

    @Override
    public boolean isConstant(Lang lang) {
        return superLang().isConstant((Expr) node(), lang);
    }

    @Override
    public Object constantValue(Lang lang) {
        return superLang().constantValue((Expr) node(), lang);
    }
}
