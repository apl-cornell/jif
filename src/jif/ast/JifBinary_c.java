package jif.ast;

import java.util.List;

import polyglot.ast.Binary;
import polyglot.ast.Binary_c;
import polyglot.ast.Expr;
import polyglot.ast.Binary.Operator;
import polyglot.types.TypeSystem;
import polyglot.util.Position;

public class JifBinary_c extends Binary_c implements Binary
{
    public JifBinary_c(Position pos, Expr left, Operator op, Expr right) {
        super(pos, left, op, right);
    }

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
                if (o instanceof Number && ((Number)o).longValue() != 0) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }    
}
