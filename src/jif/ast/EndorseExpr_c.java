package jif.ast;

import polyglot.ast.Expr;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>EndorseExpr</code> interface.
 */
public class EndorseExpr_c extends DowngradeExpr_c implements EndorseExpr {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public EndorseExpr_c(Position pos, Expr expr, LabelNode bound,
            LabelNode label) {
        super(pos, expr, bound, label);
    }

    @Override
    public String downgradeKind() {
        return "endorse";
    }

}
