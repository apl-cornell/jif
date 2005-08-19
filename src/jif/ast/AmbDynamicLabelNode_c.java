package jif.ast;

import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.SemanticDetailedException;
import jif.types.label.Label;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.*;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

/** An implementation of the <tt>AmbDynamicLabel</tt> interface. */
public class AmbDynamicLabelNode_c extends AmbLabelNode_c implements AmbDynamicLabelNode
{
    protected Expr expr;

    public AmbDynamicLabelNode_c(Position pos, Expr expr) {
	super(pos);
	this.expr = expr;
    }

    public String toString() {
	return "*" + expr + "{amb}";
    }

    /** Disambiguate the type of this node. */
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
	Context c = sc.context();
	JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
	JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();
    
	// run the typechecker over expr.
	expr = (Expr)expr.visit(new TypeChecker(sc.job(), ts, nf));
	
        if (!JifUtil.isFinalAccessExprOrConst(ts, expr)) {
            throw new SemanticDetailedException(
                "Illegal dynamic label.",
                "Only final access paths or label expressions can be used as a dynamic label. " +
                "A final access path is an expression starting with either \"this\" or a final " +
                "local variable \"v\", followed by zero or more final field accesses. That is, " +
                "a final access path is either this.f1.f2....fn, or v.f1.f2.....fn, where v is a " +
                "final local variables, and each field f1 to fn is a final field. A label expression " +
                "is either a label parameter, or a \"new label {...}\" expression.",
                this.position());
        }

        Label L = ts.dynamicLabel(position(), JifUtil.exprToAccessPath(expr, (JifContext)c));
        return nf.CanonicalLabelNode(position(), L);
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("*");
        expr.prettyPrint(w, tr);
    }
    public Node visitChildren(NodeVisitor v) {
        Expr expr = (Expr) visitChild(this.expr, v);
        if (this.expr == expr) { return this; }
        return new AmbDynamicLabelNode_c(this.position, expr); 
    }
}
