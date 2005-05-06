package jif.ast;

import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.label.Label;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
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

        if (!expr.isDisambiguated()) {
            return this;
        }

        if (!JifUtil.isFinalAccessExprOrConst(ts, expr)) {
            throw new SemanticException("Only a final access path can be used as a dynamic label.");
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
