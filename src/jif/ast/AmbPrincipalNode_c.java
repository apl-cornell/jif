package jif.ast;

import java.util.List;

import jif.types.*;
import jif.types.label.AccessPath;
import jif.types.principal.ExternalPrincipal;
import jif.types.principal.Principal;
import polyglot.ast.*;
import polyglot.frontend.MissingDependencyException;
import polyglot.frontend.Scheduler;
import polyglot.frontend.goals.Goal;
import polyglot.types.*;
import polyglot.util.Position;
import polyglot.visit.*;

/** An implementation of the <code>AmbPrincipalNode</code> interface. 
 */
public class AmbPrincipalNode_c extends PrincipalNode_c implements AmbPrincipalNode
{
    protected Expr expr;
    protected String name;
    
    public AmbPrincipalNode_c(Position pos, Expr expr) {
        super(pos);
        this.expr = expr;
        this.name = null;
    }
    
    public AmbPrincipalNode_c(Position pos, String name) {
        super(pos);
        this.expr = null;
        this.name = name;
    }

    public boolean isDisambiguated() {
        return false;
    }
    
    public String toString() {
        if (expr != null) return expr + "{amb}";
        return name + "{amb}";
    }
    
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        if (name != null) {
            return disambiguateName(ar, name);
        }
        
        // must be the case that name == null and expr != null
        if (!ar.isASTDisambiguated(expr)) {
            Scheduler sched = ar.job().extensionInfo().scheduler();
            Goal g = sched.Disambiguated(ar.job());
            throw new MissingDependencyException(g);
        }

        JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) ar.nodeFactory();

	// run the typechecker over expr.
        TypeChecker tc = new TypeChecker(ar.job(), ts, nf);
        tc = (TypeChecker) tc.context(ar.context());
        expr = (Expr)expr.visit(tc);

        if (expr.type() == null || !expr.type().isCanonical()) {
            // try converting the expression to an access path anyway,
            // to flush out any semantic errors that may arise.
            JifUtil.exprToAccessPath(expr, (JifContext)ar.context());
            
            // no errors thrown, wait until all dependencies are
            // disambiguated.
            Scheduler sched = ar.job().extensionInfo().scheduler();
            Goal g = sched.Disambiguated(ar.job());
            throw new MissingDependencyException(g);
        }

        if (!JifUtil.isFinalAccessExprOrConst(ts, expr)) {
            // illegal dynamic principal. But try to convert it to an access path
            // to allow a more precise error message.
            AccessPath ap = JifUtil.exprToAccessPath(expr, (JifContext)ar.context()); 
            ap.verify((JifContext)ar.context());

            // previous line should throw an exception, but throw this just to
            // be safe.
            throw new SemanticDetailedException(
                "Illegal dynamic principal.",
                "Only final access paths or principal expressions can be used as a dynamic principal. " +
                "A final access path is an expression starting with either \"this\" or a final " +
                "local variable \"v\", followed by zero or more final field accesses. That is, " +
                "a final access path is either this.f1.f2....fn, or v.f1.f2.....fn, where v is a " +
                "final local variables, and each field f1 to fn is a final field. A principal expression " +
                "is either a principal parameter, or an external principal.",
                this.position());                                        
        }

        return nf.CanonicalPrincipalNode(position(),
                                         ts.dynamicPrincipal(position(), JifUtil.exprToAccessPath(expr, (JifContext)ar.context())));
    }
    protected Node disambiguateName(AmbiguityRemover ar, String name) throws SemanticException {
        Context c = ar.context();
        VarInstance vi = c.findVariable(name);
        
        if (vi instanceof JifVarInstance) {
            return varToPrincipal((JifVarInstance) vi, ar);
        }
        
        if (vi instanceof PrincipalInstance) {
            return principalToPrincipal((PrincipalInstance) vi, ar);
        }
        
        if (vi instanceof ParamInstance) {
            return paramToPrincipal((ParamInstance) vi, ar);
        }
        
        throw new SemanticException(vi + " cannot be used as principal.",
                                    position());
    }
    
    protected Node varToPrincipal(JifVarInstance vi, AmbiguityRemover sc)
    throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();
        
        if (vi.flags().isFinal()) {
            return nf.CanonicalPrincipalNode(position(),
                                             ts.dynamicPrincipal(position(), JifUtil.varInstanceToAccessPath(vi, this.position())));
        }
        
        throw new SemanticException(vi + " is not a final variable " +
        "of type \"principal\".");
    }
    
    protected Node principalToPrincipal(PrincipalInstance vi,
            AmbiguityRemover sc)
    throws SemanticException {
        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();
        ExternalPrincipal ep = vi.principal();
        //((JifContext)sc.context()).ph().
        return nf.CanonicalPrincipalNode(position(), ep);
    }
    
    protected Node paramToPrincipal(ParamInstance pi, AmbiguityRemover sc)
    throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();
        
        if (pi.isPrincipal()) {
            // <param principal uid> => <principal-param uid>
            Principal p = ts.principalParam(position(), pi);
            return nf.CanonicalPrincipalNode(position(), p);
        }
        
        throw new SemanticException(pi + " may not be used as a principal.",
                                    position());
    }
    
    /**
     * Visit this term in evaluation order.
     */
    public List acceptCFG(CFGBuilder v, List succs) {
        return succs;
    }
    public Term entry() {
        return this;    
    }
    public Node visitChildren(NodeVisitor v) {
        if (this.expr == null) return this;
        
        Expr expr = (Expr) visitChild(this.expr, v);
        if (this.expr == expr) { return this; }
        return new AmbPrincipalNode_c(this.position, expr); 
    }
    
}
