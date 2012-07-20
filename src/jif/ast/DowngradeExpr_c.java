package jif.ast;

import java.util.List;

import jif.types.JifTypeSystem;
import polyglot.ast.Expr;
import polyglot.ast.Expr_c;
import polyglot.ast.Node;
import polyglot.ast.Precedence;
import polyglot.ast.Term;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.FlowGraph;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;
import polyglot.visit.TypeChecker;

/** An implementation of the <code>DowngradeExpr</code> interface.
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

    @Override
    public Expr expr() {
        return expr;
    }

    @Override
    public DowngradeExpr expr(Expr expr) {
        DowngradeExpr_c n = (DowngradeExpr_c) copy();
        n.expr = expr;
        return n;
    }

    @Override
    public LabelNode label() {
        return label;
    }

    @Override
    public DowngradeExpr label(LabelNode label) {
        DowngradeExpr_c n = (DowngradeExpr_c) copy();
        n.label = label;
        return n;
    }

    @Override
    public LabelNode bound() {
        return bound;
    }

    @Override
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

    @Override
    public Node visitChildren(NodeVisitor v) {
        Expr expr = (Expr) visitChild(this.expr, v);
        LabelNode bound = this.bound==null?null:((LabelNode) visitChild(this.bound, v));
        LabelNode label = (LabelNode) visitChild(this.label, v);
        return reconstruct(expr, bound, label);
    }

    @Override
    public Node typeCheck(TypeChecker tc) {
        return type(expr.type());
    }

    @Override
    public Term firstChild() {
        return expr;
    }

    @Override
    public <T> List<T> acceptCFG(CFGBuilder<?> v, List<T> succs) {
        JifTypeSystem ts = (JifTypeSystem)v.typeSystem();
        if (ts.Boolean().equals(ts.unlabel(expr.type()))) {
            // allow more precise dataflow when downgrading a boolean expression.
            v.visitCFG(expr, FlowGraph.EDGE_KEY_TRUE, this, EXIT,
                    FlowGraph.EDGE_KEY_FALSE, this, EXIT);
        }
        else {
            v.visitCFG(expr, this, EXIT);
        }
        return succs;
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write(downgradeKind());
        w.write("(");
        print(expr, w, tr);
        w.write(",");
        w.allowBreak(0, " ");
        print(label, w, tr);
        w.write(")");
    }

    @Override
    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }

    @Override
    public String toString() {
        return downgradeKind() + "(" + expr + ", " + label + ")";
    }

    @Override
    public Precedence precedence() {
        return expr.precedence();
    }
}
