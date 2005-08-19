package jif.ast;

import java.util.List;

import polyglot.ast.*;
import polyglot.ext.jl.ast.Expr_c;
import polyglot.types.SemanticException;
import polyglot.util.*;
import polyglot.visit.*;

/** An implemenation of the <code>DeclassifyExpr</code> interface.
 */
public class DeclassifyExpr_c extends Expr_c implements DeclassifyExpr
{
    LabelNode label;
    LabelNode bound;
    Expr expr;
    
    public DeclassifyExpr_c(Position pos, Expr expr, 
                            LabelNode bound, LabelNode label) {
        super(pos);
        this.expr = expr;
        this.bound = bound;
        this.label = label;
    }

    public Expr expr() {
	return expr;
    }

    public DeclassifyExpr expr(Expr expr) {
	DeclassifyExpr_c n = (DeclassifyExpr_c) copy();
	n.expr = expr;
	return n;
    }

    public LabelNode label() {
	return label;
    }

    public DeclassifyExpr label(LabelNode label) {
	DeclassifyExpr_c n = (DeclassifyExpr_c) copy();
	n.label = label;
	return n;
    }
    
    public LabelNode bound() {
        return bound;
    }
    
    public DeclassifyExpr bound(LabelNode b) {
        DeclassifyExpr_c n = (DeclassifyExpr_c) copy();
        n.bound = b;
        return n;
    }

    protected DeclassifyExpr_c reconstruct(Expr expr, LabelNode bound, LabelNode label) {
	if (this.expr != expr || this.bound != bound || this.label != label) {
	    DeclassifyExpr_c n = (DeclassifyExpr_c) copy();
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
        w.write("declassify(");
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
	return "declassify(" + expr + ", " + label + ")";
    }

    public Precedence precedence() {
	return expr.precedence();
    }
}
