package jif.ast;

import jif.types.*;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ext.jl.ast.Node_c;
import polyglot.frontend.MissingDependencyException;
import polyglot.frontend.Scheduler;
import polyglot.frontend.goals.Goal;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.TypeChecker;

/** An implementation of the <code>AmbParam</code> interface. 
 */
public class AmbExprParam_c extends Node_c implements AmbExprParam
{
    protected Expr expr;
    protected ParamInstance expectedPI;
    
    public AmbExprParam_c(Position pos, Expr expr, ParamInstance expectedPI) {
        super(pos);
        this.expr = expr;
        this.expectedPI = expectedPI;
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

    public AmbParam expectedPI(ParamInstance expectedPI) {
        AmbExprParam_c n = (AmbExprParam_c) copy();
        n.expectedPI = expectedPI;
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
        return new AmbExprParam_c(this.position, expr, expectedPI); 
    }
    
    /** 
     * Always return a CanoncialLabelNode, and let the dynamic label be possibly 
     * changed to a dynamic principal later.
     */
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
        if (!sc.isASTDisambiguated(expr)) {
            Scheduler sched = sc.job().extensionInfo().scheduler();
            Goal g = sched.Disambiguated(sc.job());
            throw new MissingDependencyException(g);
        }
        JifContext c = (JifContext)sc.context();
        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();

        // run the typechecker over expr.
        TypeChecker tc = new TypeChecker(sc.job(), ts, nf);
        tc = (TypeChecker) tc.context(sc.context());
	expr = (Expr)expr.visit(tc);

	if (expr instanceof PrincipalNode || 
            ts.isImplicitCastValid(expr.type(), ts.Principal()) ||
            (expectedPI != null && expectedPI.isPrincipal())) {
            if (!JifUtil.isFinalAccessExprOrConst(ts, expr)) {
                throw new SemanticDetailedException(
                    "Illegal principal parameter.",
                    "The expression " + expr + " is not suitable as a " +
                    "principal parameter. Principal parameters can be either " +
                    "dynamic principals, or principal expressions, such as a " +
                    "principal parameter, or an external principal.",
                    this.position());                                        
            }
            return nf.CanonicalPrincipalNode(position(), 
                                             JifUtil.exprToPrincipal(ts, expr, c));
        }
        if (!JifUtil.isFinalAccessExprOrConst(ts, expr)) {
            throw new SemanticDetailedException(
                "Illegal label parameter.",
                "The expression " + expr + " is not suitable as a " +
                "label parameter. Label parameters can be either " +
                "dynamic labels, or label expressions.",
                this.position());
        }
        return nf.CanonicalLabelNode(position(), 
                                     JifUtil.exprToLabel(ts, expr, c));            
    }
}
