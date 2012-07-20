package jif.ast;

import java.util.List;

import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.ast.Stmt_c;
import polyglot.ast.Term;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;

/** An implementation of the <code>DowngradeStmt</code> interface.
 */
public abstract class DowngradeStmt_c extends Stmt_c implements DowngradeStmt
{
    private LabelNode bound;
    private LabelNode label;
    private Stmt body;

    public DowngradeStmt_c(Position pos, LabelNode bound,
            LabelNode label, Stmt body) {
        super(pos);
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
        DowngradeStmt_c n = (DowngradeStmt_c) copy();
        n.label = label;
        return n;
    }

    @Override
    public LabelNode bound() {
        return bound;
    }

    @Override
    public DowngradeStmt bound(LabelNode b) {
        DowngradeStmt_c n = (DowngradeStmt_c) copy();
        n.bound = b;
        return n;
    }

    @Override
    public Stmt body() {
        return body;
    }

    @Override
    public DowngradeStmt body(Stmt body) {
        DowngradeStmt_c n = (DowngradeStmt_c) copy();
        n.body = body;
        return n;
    }

    protected DowngradeStmt_c reconstruct(LabelNode bound, LabelNode label, Stmt body) {
        if (this.bound != bound || this.label != label || this.body != body) {
            DowngradeStmt_c n = (DowngradeStmt_c) copy();
            n.bound = bound;
            n.label = label;
            n.body = body;
            return n;
        }

        return this;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        LabelNode bound = this.bound==null?null:((LabelNode) visitChild(this.bound, v));
        LabelNode label = (LabelNode) visitChild(this.label, v);
        Stmt body = (Stmt) visitChild(this.body, v);
        return reconstruct(bound, label, body);
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
        }
        else {
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
