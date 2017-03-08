package jif.ast;

import polyglot.ast.Ext;
import polyglot.ast.Stmt;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>DeclassifyStmt</code> interface.
 */
public class DeclassifyStmt_c extends DowngradeStmt_c
        implements DeclassifyStmt {
    private static final long serialVersionUID = SerialVersionUID.generate();

//    @Deprecated
    public DeclassifyStmt_c(Position pos, LabelNode bound, LabelNode label,
            Stmt body) {
        this(pos, bound, label, body, null);
    }

    public DeclassifyStmt_c(Position pos, LabelNode bound, LabelNode label,
            Stmt body, Ext ext) {
        super(pos, bound, label, body, ext);
    }

    @Override
    public String downgradeKind() {
        return "declassify";
    }
}
