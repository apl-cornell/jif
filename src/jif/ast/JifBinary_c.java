package jif.ast;

import jif.extension.JifExprExt;
import jif.visit.IntegerBoundsChecker.Interval;
import polyglot.ast.Binary;
import polyglot.ast.Binary_c;
import polyglot.ast.Expr;
import polyglot.ast.Ext;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

// XXX Should be replaced with extension
@Deprecated
public class JifBinary_c extends Binary_c implements Binary {
    private static final long serialVersionUID = SerialVersionUID.generate();

//    @Deprecated
    public JifBinary_c(Position pos, Expr left, Operator op, Expr right) {
        this(pos, left, op, right, null);
    }

    public JifBinary_c(Position pos, Expr left, Operator op, Expr right,
            Ext ext) {
        super(pos, left, op, right, ext);
    }

    @Override
    public boolean throwsArithmeticException() {
        if (op == DIV || op == MOD) {
            // it's a divide or mod operation.
            if (right.type().isFloat() || right.type().isDouble()) {
                // floats and doubles don't throw
                return false;
            }
            if (right.isConstant()) {
                // is it a non-zero constant?
                Object o = right.constantValue();
                if (o instanceof Number && ((Number) o).longValue() != 0) {
                    return false;
                }
            }
            if (((JifExprExt) JifUtil.jifExt(right()))
                    .getNumericBounds() != null) {
                Interval i = ((JifExprExt) JifUtil.jifExt(right()))
                        .getNumericBounds();
                if ((i.getLower() != null && i.getLower().longValue() > 0)
                        || (i.getUpper() != null
                                && i.getUpper().longValue() < 0)) {
                    // the right operand is non zero
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
