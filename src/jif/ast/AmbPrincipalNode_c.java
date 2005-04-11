package jif.ast;

import java.util.List;

import jif.parse.Name;
import jif.parse.Wrapper;
import jif.types.*;
import jif.types.principal.*;
import jif.types.principal.DynamicPrincipal_c;
import jif.types.principal.ExternalPrincipal;
import jif.types.principal.Principal;
import polyglot.ast.*;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.*;
import polyglot.util.Position;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.CFGBuilder;
import polyglot.visit.NodeVisitor;

/** An implementation of the <code>AmbPrincipalNode</code> interface. 
 */
public class AmbPrincipalNode_c extends PrincipalNode_c implements AmbPrincipalNode
{
    protected Expr expr;
    
    public AmbPrincipalNode_c(Position pos, Expr expr) {
        super(pos);
        this.expr = expr;
    }
    
    public boolean isDisambiguated() {
        return false;
    }
    
    public String toString() {
        return expr + "{amb}";
    }
    
//    public NodeVisitor disambiguateEnter(AmbiguityRemover ar) throws SemanticException {
//        if (expr instanceof Wrapper) {
//            // avoiding visiting the child if it is just a single name.
//            return ar.bypassChildren(this);
//        }
//        return ar;
//    }
    
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
        if (expr instanceof Wrapper) {
            Wrapper w = (Wrapper)expr;
            Name n = (Name)w.amb;
            return disambiguateName(sc, n.name);
        }
        
        if (!expr.isDisambiguated()) {
            return this;
        }
        
        System.out.println("Disambiguating " + expr + " :: " + expr.isDisambiguated());
        System.out.println("          " + expr.getClass());
        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();
        if (!JifUtil.isFinalAccessExprOrConst(ts, expr)) {
            throw new SemanticException("Only a final access path or constant can be used as a dynamic principal.");
        }
        return nf.CanonicalPrincipalNode(position(),
                                         ts.dynamicPrincipal(position(), JifUtil.exprToAccessPath(expr, sc.context().currentClass())));
    }
    protected Node disambiguateName(AmbiguityRemover sc, String name) throws SemanticException {
        Context c = sc.context();
        VarInstance vi = c.findVariable(name);
        if (!vi.isCanonical()) {
            // the instance is not yet ready
            return this;
        }
        
        if (vi instanceof JifVarInstance) {
            return varToPrincipal((JifVarInstance) vi, sc);
        }
        
        if (vi instanceof PrincipalInstance) {
            return principalToPrincipal((PrincipalInstance) vi, sc);
        }
        
        if (vi instanceof ParamInstance) {
            return paramToPrincipal((ParamInstance) vi, sc);
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
                                             ts.dynamicPrincipal(position(), JifUtil.varInstanceToAccessPath(vi)));
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
}
