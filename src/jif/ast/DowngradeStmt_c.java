package jif.ast;

import java.util.List;

import polyglot.ast.*;
import polyglot.ext.jl.ast.Stmt_c;
import polyglot.util.*;
import polyglot.visit.*;

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

    public LabelNode label() {
	return label;
    }

    public DowngradeStmt label(LabelNode label) {
	DowngradeStmt_c n = (DowngradeStmt_c) copy();
	n.label = label;
	return n;
    }

    public LabelNode bound() {
	return bound;
    }

    public DowngradeStmt bound(LabelNode b) {
	DowngradeStmt_c n = (DowngradeStmt_c) copy();
	n.bound = b;
	return n;
    }    
    
    public Stmt body() {
	return body;
    }

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

    public Node visitChildren(NodeVisitor v) {
        LabelNode bound = (LabelNode) visitChild(this.bound, v);
	LabelNode label = (LabelNode) visitChild(this.label, v);
	Stmt body = (Stmt) visitChild(this.body, v);
	return reconstruct(bound, label, body);
    }

    public Term entry() {
        return body.entry();
    }

    public List acceptCFG(CFGBuilder v, List succs) {
        v.visitCFG(body, this);
        return succs;
    }

    public String toString() {
	return downgradeKind() + "(" + bound + ", " + label + ") " + body;
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write(downgradeKind());
        w.write("(");
        print(bound, w, tr);
        w.write(", ");
        print(label, w, tr);
        w.write(") ");
        printSubStmt(body, w, tr);
    }
    /**
     * 
     * @return Name of the kind of downgrade, e.g. "declassify" or "endorse"
     */
    protected abstract String downgradeKind();

    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }
}
