package jif.ast;

import java.util.List;

import jif.types.*;
import jif.types.label.AccessPathRoot;
import jif.types.principal.ExternalPrincipal;
import jif.types.principal.Principal;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.*;
import polyglot.util.Position;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.CFGBuilder;

/** An implementation of the <code>AmbPrincipalNode</code> interface. 
 */
public class AmbPrincipalNode_c extends PrincipalNode_c implements AmbPrincipalNode
{
    protected String name;

    public AmbPrincipalNode_c(Position pos, String name) {
	super(pos);
	this.name = name;
    }

    public boolean isDisambiguated() {
        return false;
    }

    public String name() {
	return this.name;
    }

    public String toString() {
	return name + "{amb}";
    }

    public AmbPrincipalNode name(String name) {
	AmbPrincipalNode_c n = (AmbPrincipalNode_c) copy();
	n.name = name;
	return n;
    }

    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
    	Context c = sc.context();
//        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
//        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();

        	
	//also can find out whether "name" is an external principal
        VarInstance vi = c.findVariable(name);
//@@@@@        int ind = jar.indexForArg(name);
//        
//        if (vi == null || ind >= 0) {
//            Formal formal = jar.formalForArg(ind);
//            if (formal != null && (vi == null || vi == formal.localInstance())) {
//                JifLocalInstance li = (JifLocalInstance) formal.localInstance();
//                Principal p = ts.argPrincipal(position(), li.uid(),
//                    name(), li.label(), ind, true);
//    
//                return nf.CanonicalPrincipalNode(position(), p);
//            }
//                
//            if (vi == null) {
//                // the following will fail with an appropriate exception
//                c.findVariable(name);
//            }
//        }
	
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
	    if (ts.isPrincipal(vi.type())) {
		return nf.CanonicalPrincipalNode(position(),
		    ts.dynamicPrincipal(position(), JifUtil.varInstanceToAcessPath(vi)));
	    }
	}

	throw new SemanticException("Only final variables of type " +
	    "\"principal\" may be used as principals.", position());
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
