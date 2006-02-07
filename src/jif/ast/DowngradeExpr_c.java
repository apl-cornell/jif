package jif.ast;

import java.util.List;

import polyglot.ast.*;
import polyglot.ext.jl.ast.Expr_c;
import polyglot.types.SemanticException;
import polyglot.util.*;
import polyglot.visit.*;

/** An implemenation of the <code>DowngradeExpr</code> interface.
 */
public abstract class DowngradeExpr_c extends Expr_c implements DowngradeExpr
{
    private LabelNode label;
    private LabelNode bound;
    private Expr expr;
    
    public DowngradeExpr_c(Position pos, Expr expr, 
                            LabelNode bound, LabelNode label) {
        super(pos);
        this.expr = expr;
        this.bound = bound;
        this.label = label;
    }

    public Expr expr() {
	return expr;
    }

    public DowngradeExpr expr(Expr expr) {
	DowngradeExpr_c n = (DowngradeExpr_c) copy();
	n.expr = expr;
	return n;
    }

    public LabelNode label() {
	return label;
    }

    public DowngradeExpr label(LabelNode label) {
	DowngradeExpr_c n = (DowngradeExpr_c) copy();
	n.label = label;
	return n;
    }
    
    public LabelNode bound() {
        return bound;
    }
    
    public DowngradeExpr bound(LabelNode b) {
        DowngradeExpr_c n = (DowngradeExpr_c) copy();
        n.bound = b;
        return n;
    }

    protected DowngradeExpr_c reconstruct(Expr expr, LabelNode bound, LabelNode label) {
	if (this.expr != expr || this.bound != bound || this.label != label) {
	    DowngradeExpr_c n = (DowngradeExpr_c) copy();
	    n.expr = expr;
            n.bound = bound;
            n.label = label;
	    return n;
	}

	return this;
    }

    public Node visitChildren(NodeVisitor v) {
	Expr expr = (Expr) visitChild(this.expr, v);
        LabelNode bound = (LabelNode) visitChild(this.bound, v);
        LabelNode label = (LabelNode) visitChild(this.label, v);
	return reconstruct(expr, bound, label);
    }

    public Node typeCheck(TypeChecker tc) throws SemanticException {
	return type(expr.type());
    }

    public Term entry() {
        return expr.entry();
    }

    public List acceptCFG(CFGBuilder v, List succs) {
        v.visitCFG(expr, this);
        return succs;
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write(downgradeKind());
        w.write("(");
        print(expr, w, tr);
        w.write(",");
        w.allowBreak(0, " ");
        print(label, w, tr);
        w.write(")");
    }
    
    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }

    public String toString() {
	return downgradeKind() + "(" + expr + ", " + label + ")";
    }

    public Precedence precedence() {
	return expr.precedence();
    }
}
