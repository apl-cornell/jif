package jif.ast;

import java.util.List;

import jif.types.*;
import jif.types.principal.ExternalPrincipal;
import jif.types.principal.Principal;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.Position;
import polyglot.visit.*;

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
    
    public Node disambiguateOverride(Node parent, AmbiguityRemover ar) throws SemanticException {
        // if the expression is just a single name, we disambiguate using
        // the method disambiguateName, since it could be a param or an external principal
        if (expr instanceof AmbExpr) {
            AmbExpr ae = (AmbExpr)expr;
            return disambiguateName(ar, ae.name());
        }
        return null;
    }
    
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
//        // if expression contains any ambiguous nodes, do nothing...
//        final boolean[] allOk = new boolean[] { true };
//        expr.visit(new NodeVisitor() {
//            public Node override(Node parent, Node n) {
//                if (! allOk[0]) { return n; }
//                
//                System.out.println("  " + n + " :: " + n.getClass());
//                // Don't check if New is disambiguated; this is handled
//                // during type-checking.
//                if (n instanceof Ambiguous) {
//                    allOk[0] = false;
//                    return n;
//                }
//                
//                return null;
//            }
//        });
//        
//        if (!allOk[0]) {
//            return this;
//        }

        if (!expr.isDisambiguated()) {
            return this;
        }

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
        Expr expr = (Expr) visitChild(this.expr, v);
        if (this.expr == expr) { return this; }
        return new AmbPrincipalNode_c(this.position, expr); 
    }
    
}
