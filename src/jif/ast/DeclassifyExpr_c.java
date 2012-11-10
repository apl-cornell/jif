package jif.ast;

import polyglot.ast.Expr;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>DeclassifyExpr</code> interface.
 */
public class DeclassifyExpr_c extends DowngradeExpr_c implements DeclassifyExpr {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public DeclassifyExpr_c(Position pos, Expr expr, LabelNode bound,
            LabelNode label) {
        super(pos, expr, bound, label);

    }

    @Override
    public String downgradeKind() {
        return "declassify";
    }
}
