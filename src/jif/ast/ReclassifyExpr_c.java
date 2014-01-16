/* begin-new */

package jif.ast;

import java.util.List;

import jif.types.JifTypeSystem;
import polyglot.ast.Expr;
import polyglot.ast.Expr_c;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.Precedence;
import polyglot.ast.Term;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.CFGBuilder;
import polyglot.visit.FlowGraph;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;
import polyglot.visit.TypeChecker;

/** An implementation of the <code>ReclassifyExpr</code> interface.
 *

/* Probably this extension should change */

public class ReclassifyExpr_c extends Expr_c implements ReclassifyExpr {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private Id name;
    protected Expr expr;

    public ReclassifyExpr_c(Position pos, Expr expr, Id name) {
        super(pos);
        this.name = name;
        this.expr = expr;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        Expr expr = (Expr) visitChild(this.expr, v);
        Id name = (Id) visitChild(this.name, v);
        return reconstruct(expr, name);
    }

    protected Node reconstruct(Expr expr, Id name) {
        if (this.expr == expr && this.name == name) return this;
        ReclassifyExpr_c nre = (ReclassifyExpr_c) copy();
        nre.expr = expr;
        nre.name = name;
        return nre;
    }

    @Override
    public String downgradeKind() {
        return "reclassify";
    }

    @Override
    public Expr expr() {
        return expr;
    }

    @Override
    public ReclassifyExpr expr(Expr expr) {
        ReclassifyExpr_c n = (ReclassifyExpr_c) copy();
        n.expr = expr;
        return n;
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
        JifTypeSystem ts = (JifTypeSystem) v.typeSystem();
        if (ts.Boolean().equals(ts.unlabel(expr.type()))) {
            // allow more precise dataflow when downgrading a boolean expression.
            v.visitCFG(expr, FlowGraph.EDGE_KEY_TRUE, this, EXIT,
                    FlowGraph.EDGE_KEY_FALSE, this, EXIT);
        } else {
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
        print(name, w, tr);
        w.write(")");
    }

    @Override
    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("cannot translate " + this);
    }

    @Override
    public String toString() {
        return downgradeKind() + "(" + expr + ", " + name + ")";
    }

    @Override
    public Precedence precedence() {
        return expr.precedence();
    }
}

/* end-new */
