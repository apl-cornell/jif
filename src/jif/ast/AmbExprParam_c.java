package jif.ast;

import jif.types.*;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.*;
import polyglot.ext.jl.ast.Node_c;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;

/** An implementation of the <code>AmbParam</code> interface. 
 */
public class AmbExprParam_c extends Node_c implements AmbParam
{
    protected Expr expr;

    public AmbExprParam_c(Position pos, Expr expr) {
	super(pos);
	this.expr = expr;
    }

    public boolean isDisambiguated() {
        return false;
    }

    public Expr expr() {
	return this.expr;
    }

    public AmbParam expr(Expr expr) {
	AmbExprParam_c n = (AmbExprParam_c) copy();
	n.expr = expr;
	return n;
    }

    public Param parameter() {
        throw new InternalCompilerError("No parameter yet");
    }

    public String toString() {
	return expr + "{amb}";
    }

    public Node visitChildren(NodeVisitor v) {
        Expr expr = (Expr) visitChild(this.expr, v);
        if (this.expr == expr) { return this; }
        return new AmbExprParam_c(this.position, expr); 
    }

    /** 
     * Always return a CanoncialLabelNode, and let the dynamic label be possibly 
     * changed to a dynamic principal later.
     */
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
        if (!expr.isDisambiguated()) {
            System.out.println("The expression " + expr + " is not disamb yet: " + expr.getClass());
            return this;
        }
	Context c = sc.context();
	JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
	JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();

	Label L = ts.dynamicLabel(position(), JifUtil.exprToAccessPath(expr, (JifContext)c));
        return nf.CanonicalLabelNode(position(), L);
    }
}
