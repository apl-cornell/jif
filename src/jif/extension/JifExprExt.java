package jif.extension;

import jif.ast.JifExt_c;
import jif.translate.ToJavaExt;
import jif.visit.IntegerBoundsChecker;
import polyglot.util.SerialVersionUID;

/** The Jif extension for all <code>Expr</code> nodes. 
 */
public class JifExprExt extends JifExt_c {
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
}
