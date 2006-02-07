package jif.ast;

import polyglot.ast.Expr;
import polyglot.util.Position;

/** An implemenation of the <code>EndorseExpr</code> interface.
 */
public class EndorseExpr_c extends DowngradeExpr_c implements EndorseExpr
{
    
    public EndorseExpr_c(Position pos, Expr expr, 
                            LabelNode bound, LabelNode label) {
        super(pos, expr, bound, label);
    }

    public String downgradeKind() {
        return "endorse";
    }

}
