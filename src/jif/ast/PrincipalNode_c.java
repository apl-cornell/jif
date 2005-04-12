package jif.ast;

import jif.types.*;
import jif.types.label.AccessPath;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
import polyglot.ast.Node;
import polyglot.ext.jl.ast.Expr_c;
import polyglot.types.SemanticException;
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

    public Principal principal() {
	return principal;
    }

    public PrincipalNode principal(Principal principal) {
	PrincipalNode_c n = (PrincipalNode_c) copy();
	n.principal = principal;
	return n;
    }

    public Param parameter() {
	return principal();
    }
    
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

    public boolean isDisambiguated() {
        return principal != null && principal.isCanonical() && super.isDisambiguated();
    }
    
    /** Type check the expression. */
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();
        
        if (principal instanceof DynamicPrincipal) {
            // Make sure that the access path is set correctly
            // check also that all field accesses are final, and that
            // the type of the expression is principal
            AccessPath path = ((DynamicPrincipal)principal).path();
            try {
                path.verify((JifContext)tc.context());                
            }
            catch (SemanticException e) {
                throw new SemanticException(e.getMessage(), this.position());
            }
            //@@@@@ Check that expression is of type principal?
            
        }
        return type(ts.Principal());
    }
}
