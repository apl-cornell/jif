package jif.ast;

import java.util.List;

import jif.types.JifTypeSystem;
import jif.types.principal.Principal;
import polyglot.ast.Expr_c;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.Position;
import polyglot.visit.TypeChecker;

/** An implementation of the <code>PrincipalNode</code> interface. 
 */
public abstract class PrincipalNode_c extends Expr_c implements PrincipalNode
{
    Principal principal;

    public PrincipalNode_c(Position pos) {
	super(pos);
    }

    @Override
    public Principal principal() {
	return principal;
    }

    @Override
    public PrincipalNode principal(Principal principal) {
	PrincipalNode_c n = (PrincipalNode_c) copy();
	n.principal = principal;
	return n;
    }

    @Override
    public Principal parameter() {
	return principal();
    }
    
    @Override
    public PrincipalNode parameter(Principal principal) {
        return principal(principal);
    }
    
    @Override
    public String toString() {
	if (principal != null) {
	    return principal.toString();
	}
	else {
	    return "<unknown-principal>";
	}
    }
//    public Object constantValue() {
//        return principal;
//    }

    @Override
    public boolean isDisambiguated() {
        return principal != null && principal.isCanonical() && super.isDisambiguated();
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public List throwTypes(TypeSystem ts) {
        return principal().throwTypes(ts);
    }

    /**
     * Type check the expression.
     *  
     * @throws SemanticException
     */
    @Override
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();
        return type(ts.Principal());
    }

    @Override
    public Term firstChild() {
        return null;
    }
}
