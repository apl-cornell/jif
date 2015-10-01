package jif.extension;

import java.util.LinkedList;
import java.util.List;

import jif.ast.JifUtil;
import jif.visit.IntegerBoundsChecker.Interval;
import polyglot.ast.Assign;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.SerialVersionUID;
import polyglot.util.SubtypeSet;

public class JifAssignDel extends JifDel_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected boolean arithmeticExcIsFatal = false;

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        List<Type> l = new LinkedList<Type>();

        if (throwsArithmeticException()
                && !fatalExceptions.contains(ts.ArithmeticException())) {
            l.add(ts.ArithmeticException());
        }

        return l;
    }

    public boolean throwsArithmeticException() {
        if (arithmeticExcIsFatal) return false;

        Assign a = (Assign) this.node();
        if (a.operator() == Assign.DIV_ASSIGN
                || a.operator() == Assign.MOD_ASSIGN) {
            // it's a divide or mod operation.
            if (a.right().type().isFloat() || a.right().type().isDouble()) {
                // floats and doubles don't throw
                return false;
            }
            if (a.right().isConstant()) {
                // is it a non-zero constant?
                Object o = a.right().constantValue();
                if (o instanceof Number && ((Number) o).longValue() != 0) {
                    return false;
                }
            }
            if (((JifExprExt) JifUtil.jifExt(a.right()))
                    .getNumericBounds() != null) {
                Interval i = ((JifExprExt) JifUtil.jifExt(a.right()))
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

    @Override
    public void setFatalExceptions(TypeSystem ts, SubtypeSet fatalExceptions) {
        super.setFatalExceptions(ts, fatalExceptions);
        if (fatalExceptions.contains(ts.ArithmeticException()))
            arithmeticExcIsFatal = true;
    }

}
