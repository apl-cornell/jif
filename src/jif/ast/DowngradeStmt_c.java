package jif.ast;

import java.util.List;

import polyglot.ast.Ext;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.ast.Stmt_c;
import polyglot.ast.Term;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.CFGBuilder;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;

/** An implementation of the <code>DowngradeStmt</code> interface.
 */
public abstract class DowngradeStmt_c extends Stmt_c implements DowngradeStmt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected LabelNode bound;
    protected LabelNode label;
    protected Stmt body;

    @Deprecated
    public DowngradeStmt_c(Position pos, LabelNode bound, LabelNode label,
            Stmt body) {
        this(pos, bound, label, body, null);
    }

    public DowngradeStmt_c(Position pos, LabelNode bound, LabelNode label,
            Stmt body, Ext ext) {
        super(pos, ext);
        this.bound = bound;
        this.label = label;
        this.body = body;
    }

    @Override
    public LabelNode label() {
        return label;
    }

    @Override
    public DowngradeStmt label(LabelNode label) {
        return label(this, label);
    }

    protected <N extends DowngradeStmt_c> N label(N n, LabelNode label) {
        if (n.label == label) return n;
        n = copyIfNeeded(n);
        n.label = label;
        return n;
    }

    @Override
    public LabelNode bound() {
        return bound;
    }

    @Override
    public DowngradeStmt bound(LabelNode b) {
        return bound(this, b);
    }

    protected <N extends DowngradeStmt_c> N bound(N n, LabelNode bound) {
        if (n.bound == bound) return n;
        n = copyIfNeeded(n);
        n.bound = bound;
        return n;
    }

    @Override
    public Stmt body() {
        return body;
    }

    @Override
    public DowngradeStmt body(Stmt body) {
        return body(this, body);
    }

    protected <N extends DowngradeStmt_c> N body(N n, Stmt body) {
        if (n.body == body) return n;
        n = copyIfNeeded(n);
        n.body = body;
        return n;
    }

    protected <N extends DowngradeStmt_c> N reconstruct(N n, LabelNode bound,
            LabelNode label, Stmt body) {
        n = bound(n, bound);
        n = label(n, label);
        n = body(n, body);
        return n;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        LabelNode bound = this.bound == null ? null
                : ((LabelNode) visitChild(this.bound, v));
        LabelNode label = visitChild(this.label, v);
        Stmt body = visitChild(this.body, v);
        return reconstruct(this, bound, label, body);
    }

    @Override
    public Term firstChild() {
        return body;
    }

    @Override
    public <T> List<T> acceptCFG(CFGBuilder<?> v, List<T> succs) {
        v.visitCFG(body, this, EXIT);
        return succs;
    }

    @Override
    public String toString() {
        if (bound == null) {
            return downgradeKind() + "(" + label + ") " + body;
        } else {
            return downgradeKind() + "(" + bound + " to " + label + ") " + body;
        }
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write(downgradeKind());
        w.write("(");
        if (bound != null) {
            print(bound, w, tr);
            w.write(" to ");
        }
        print(label, w, tr);
        w.write(") ");
        printSubStmt(body, w, tr);
    }

    /**
     * 
     * @return Name of the kind of downgrade, e.g. "declassify" or "endorse"
     */
    @Override
    public abstract String downgradeKind();

    @Override
    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }
}
