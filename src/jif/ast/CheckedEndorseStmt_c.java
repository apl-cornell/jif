package jif.ast;

import polyglot.ast.Expr;
import polyglot.ast.Ext;
import polyglot.ast.If;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

/** An implementation of the <code>CheckedEndorseStmt</code> interface.
 */
public class CheckedEndorseStmt_c extends EndorseStmt_c
        implements CheckedEndorseStmt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected Expr expr;

//    @Deprecated
    public CheckedEndorseStmt_c(Position pos, Expr e, LabelNode bound,
            LabelNode label, If body) {
        this(pos, e, bound, label, body, null);
    }

    public CheckedEndorseStmt_c(Position pos, Expr e, LabelNode bound,
            LabelNode label, If body, Ext ext) {
        super(pos, bound, label, body, ext);
        this.expr = e;
    }

    @Override
    public Expr expr() {
        return expr;
    }

    @Override
    public CheckedEndorseStmt expr(Expr expr) {
        return expr(this, expr);
    }

    protected <N extends CheckedEndorseStmt_c> N expr(N n, Expr expr) {
        if (n.expr == expr) return n;
        n = copyIfNeeded(n);
        n.expr = expr;
        return n;
    }

    protected <N extends CheckedEndorseStmt_c> N reconstruct(N n, Expr expr,
            LabelNode bound, LabelNode label, Stmt body) {
        n = super.reconstruct(n, bound, label, body);
        n = expr(n, expr);
        return n;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        Expr expr = visitChild(this.expr(), v);
        LabelNode bound = this.bound() == null ? null
                : ((LabelNode) visitChild(this.bound(), v));
        LabelNode label = visitChild(this.label(), v);
        Stmt body = visitChild(this.body(), v);
        return reconstruct(this, expr, bound, label, body);
    }

    @Override
    public String toString() {
        return downgradeKind() + "(" + expr() + ", "
                + (bound() == null ? "" : (bound() + " to ")) + label() + ") "
                + body();
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write(downgradeKind());
        w.write("(");
        print(expr, w, tr);
        w.write(", ");
        if (bound() != null) {
            print(bound(), w, tr);
            w.write(" to ");
        }
        print(label(), w, tr);
        w.write(") ");
        printSubStmt(body(), w, tr);
    }

}
