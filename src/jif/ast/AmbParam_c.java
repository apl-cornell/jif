package jif.ast;

import jif.types.*;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.Node;
import polyglot.ext.jl.ast.Node_c;
import polyglot.types.*;
import polyglot.util.Position;
import polyglot.visit.AmbiguityRemover;

/** An implementation of the <code>AmbParam</code> interface. 
 */
public class AmbParam_c extends Node_c implements AmbParam
{
    protected String name;
    protected Param param;

    public AmbParam_c(Position pos, String name) {
	super(pos);
	this.name = name;
    }

    public boolean isDisambiguated() {
        return false;
    }

    public String name() {
	return this.name;
    }

    public AmbParam name(String name) {
	AmbParam_c n = (AmbParam_c) copy();
	n.name = name;
	return n;
    }

    public Param parameter() {
	return this.param;
    }

    public AmbParam parameter(Param param) {
	AmbParam_c n = (AmbParam_c) copy();
	n.param = param;
	return n;
    }

    public String toString() {
	return name + "{amb}";
    }

    /** Disambiguates <code>name</code>.
     *  If <code>name</code> is a <tt>VarInstance</tt>, we get a dynamic label/principal
     *  node. If <code>name</code> is a <tt>PrincipalInstance</tt>, we get the same 
     *  principal. If <code>name</code> is a <tt>ParamInstance</tt>, we get a <tt>ParamLabel</tt>
     *  or a <tt>ParamPrincipal</tt>.
     */
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
	Context c = sc.context();
//        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
//        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();
            
        VarInstance vi = c.findVariable(name);
        //        int ind = jar.indexForArg(name);
//        if (vi == null || ind >= 0) {
//            Formal formal = jar.formalForArg(ind);
//            if (formal != null && (vi == null || vi == formal.localInstance())) {
//                JifProcedureInstance pi = (JifProcedureInstance)c.currentCode();
//                JifLocalInstance li = (JifLocalInstance) formal.localInstance();
//            
//                if (ts.isPrincipal(formal.declType())) {
//                    Principal p = ts.argPrincipal(position(), 
//                    li.uid(), name(), li.label(), 
//                    ind, true);
//                    return nf.CanonicalPrincipalNode(position(), p);
//                }
//                else if (ts.isLabel(formal.declType())) {
//                    Label L = pi.getArgLabel(ind); //###
//                    return nf.CanonicalLabelNode(position(), L);
//                }
//            }
//                    
//            // the following will fail with an appropriate exception
//            if (vi == null) {
//                c.findVariable(name);
//            }
//        }

        if (!vi.isCanonical()) {
            // not yet ready to disambiguate
            return this;
        }
	if (vi instanceof JifVarInstance) {
	    return varToParam((JifVarInstance) vi, sc);
	}

	if (vi instanceof PrincipalInstance) {
	    return principalToParam((PrincipalInstance) vi, sc);
	}

	if (vi instanceof ParamInstance) {
	    return paramToParam((ParamInstance) vi, sc);
	}

	throw new SemanticException(vi + " cannot be used as parameter.");
    }

    /** Turns a <code>JifVarInstance</code> object into a label node or
     *  a principal node */
    protected Node varToParam(JifVarInstance vi, AmbiguityRemover sc)
	throws SemanticException {

	JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
	JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();

	if (vi.flags().isFinal()) {
	    if (ts.isLabel(vi.type())) {
		return nf.CanonicalLabelNode(position(), vi.label());
	    }

	    if (ts.isPrincipal(vi.type())) {
		Principal p = ts.dynamicPrincipal(position(), JifUtil.varInstanceToAccessPath(vi));
		return nf.CanonicalPrincipalNode(position(), p);
	    }
	}

	throw new SemanticException("Only final variables of type \"label\" " +
	    "or \"principal\" may be used as class parameters.", position());
    }

    /** Turns a <code>PrincipalInstance</code> object into a principal node. */
    protected Node principalToParam(PrincipalInstance vi, AmbiguityRemover sc)
	throws SemanticException {
	JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();
	return nf.CanonicalPrincipalNode(position(), vi.principal());
    }

    /** Turns a <code>PramaInstance</code> object into a label node or a 
     *  principal node. 
     */
    protected Node paramToParam(ParamInstance pi, AmbiguityRemover sc)
	throws SemanticException {
	JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
	JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();

	if (pi.isCovariantLabel()) {
	    // <covariant label uid> => <covariant-label uid>
	    Label L = ts.covariantLabel(position(), pi);
	    return nf.CanonicalLabelNode(position(), L);
	}

	if (pi.isInvariantLabel()) {
	    // <param label uid> => <label-param uid>
	    Label L = ts.paramLabel(position(), pi);
            L.setDescription("label parameter " + pi.name() + 
                                   " of class " + pi.container().fullName());
	    return nf.CanonicalLabelNode(position(), L);
	}

	if (pi.isPrincipal()) {
	    // <param principal uid> => <principal-param uid>
	    Principal p = ts.principalParam(position(), pi);
	    return nf.CanonicalPrincipalNode(position(), p);
	}

	throw new SemanticException("Unrecognized parameter type for " + pi,
		                    position());
    }
}
