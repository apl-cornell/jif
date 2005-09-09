package jif.ast;

import jif.types.*;
import jif.types.label.AccessPath;
import jif.types.label.Label;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.frontend.MissingDependencyException;
import polyglot.frontend.Scheduler;
import polyglot.frontend.goals.Goal;
import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.*;

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

    private int expr_disamb_fail_count = 0;
    private static final int EXPR_DISAMB_FAIL_LIMIT = 40;
    
    /** Disambiguate the type of this node. */
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
	Context c = sc.context();
	JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
	JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();
    
        if (!sc.isASTDisambiguated(expr)) {
            Scheduler sched = sc.job().extensionInfo().scheduler();
            Goal g = sched.Disambiguated(sc.job());
            throw new MissingDependencyException(g);
        }

        // run the typechecker over expr.
        TypeChecker tc = new TypeChecker(sc.job(), ts, nf);
        tc = (TypeChecker) tc.context(sc.context());
        expr = (Expr)expr.visit(tc);
	
        if (expr.type() == null || !expr.type().isCanonical()) {
            if (++expr_disamb_fail_count < EXPR_DISAMB_FAIL_LIMIT) {
                // keep trying until we exhaust our patience.
                // needed for some cases where we can't distinguish if
                // we need to push on regardless, or wait until the type
                // really can be resolved.
                Scheduler sched = sc.job().extensionInfo().scheduler();
                Goal g = sched.Disambiguated(sc.job());
                throw new MissingDependencyException(g);
            }
        }

        if (expr.type() != null && expr.type().isCanonical() && 
                !JifUtil.isFinalAccessExprOrConst(ts, expr)) {
            // illegal dynamic label. But try to convert it to an access path
            // to allow a more precise error message.
            AccessPath ap = JifUtil.exprToAccessPath(expr, (JifContext)c); 
            ap.verify((JifContext)c);

            // previous line should throw an exception, but throw this just to
            // be safe.
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

        // the expression type may not yet be fully determined, but
        // that's ok, as type checking will ensure that it is
        // a suitable expression.
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
