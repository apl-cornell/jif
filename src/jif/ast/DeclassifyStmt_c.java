package jif.ast;

import java.util.List;

import polyglot.ast.*;
import polyglot.ext.jl.ast.Stmt_c;
import polyglot.util.*;
import polyglot.visit.*;

/** An implementation of the <code>DeclassifyStmt</code> interface.
 */
public class DeclassifyStmt_c extends Stmt_c implements DeclassifyStmt
{
    LabelNode bound;
    LabelNode label;
    Stmt body;

    public DeclassifyStmt_c(Position pos, LabelNode bound,
                            LabelNode label, Stmt body) {
	super(pos);
        this.bound = bound;
	this.label = label;
	this.body = body;
    }

    public LabelNode label() {
	return label;
    }

    public DeclassifyStmt label(LabelNode label) {
	DeclassifyStmt_c n = (DeclassifyStmt_c) copy();
	n.label = label;
	return n;
    }

    public LabelNode bound() {
	return bound;
    }

    public DeclassifyStmt bound(LabelNode b) {
	DeclassifyStmt_c n = (DeclassifyStmt_c) copy();
	n.bound = b;
	return n;
    }    
    
    public Stmt body() {
	return body;
    }

    public DeclassifyStmt body(Stmt body) {
	DeclassifyStmt_c n = (DeclassifyStmt_c) copy();
	n.body = body;
	return n;
    }

    protected DeclassifyStmt_c reconstruct(LabelNode bound, LabelNode label, Stmt body) {
	if (this.bound != bound || this.label != label || this.body != body) {
	    DeclassifyStmt_c n = (DeclassifyStmt_c) copy();
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
	return "declassify(" + bound + ", " + label + ") " + body;
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("declassify (");
        print(bound, w, tr);
        w.write(", ");
        print(label, w, tr);
        w.write(") ");
        printSubStmt(body, w, tr);
    }

    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }
}
