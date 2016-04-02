package jif.ast;

import polyglot.ast.Expr;
import polyglot.ast.Ext;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>DeclassifyExpr</code> interface.
 */
public class DeclassifyExpr_c extends DowngradeExpr_c
        implements DeclassifyExpr {
    private static final long serialVersionUID = SerialVersionUID.generate();

//    @Deprecated
    public DeclassifyExpr_c(Position pos, Expr expr, LabelNode bound,
            LabelNode label) {
        this(pos, expr, bound, label, null);
    }

    public DeclassifyExpr_c(Position pos, Expr expr, LabelNode bound,
            LabelNode label, Ext ext) {
        super(pos, expr, bound, label, ext);

    }

    @Override
    public String downgradeKind() {
        return "declassify";
    }
}
