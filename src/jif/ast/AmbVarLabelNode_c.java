package jif.ast;

import jif.types.*;
import polyglot.ast.Node;
import polyglot.types.*;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.PrettyPrinter;

/** An implementation of the <code>AmbVarLabelNode</code> interface. 
 */
public class AmbVarLabelNode_c extends AmbLabelNode_c
                              implements AmbVarLabelNode
{
    protected String name;

    public AmbVarLabelNode_c(Position pos, String name) {
	super(pos);
	this.name = name;
    }

    public String toString() {
	return name + "{amb}";
    }

    public String name() {
	return this.name;
    }

    public AmbVarLabelNode name(String name) {
	AmbVarLabelNode_c n = (AmbVarLabelNode_c) copy();
	n.name = name;
	return n;
    }

    /** Disambiguates the type of this node. The following rules are used:
     *  Suppose <code>vi</code> is the variable instance corresponding to <tt>name</tt>,
     *  and <code>label</code> represents the Jif label contained in this node,
     *  <ul> 
     *  <li>if <tt>vi</tt> is a <tt>JifVarInstance</tt>, <tt>label</tt> ought to be
     *  <tt>vi.label()</tt>. </li>
     *  <li>if <tt>vi</tt> is a <tt>ParamInstance</tt>, 
     *	    <ul> <li>if <tt>vi</tt> is an covariant label parameter, <tt>label</tt> ought
     *		 to be a covariant label with the same <tt>uid</tt> as <tt>vi</tt>. </li>
     *		 <li>if <tt>vi</tt> is an invariant label parameter, <tt>label</tt> ought
     *		 to be a <tt>ParamLabel</tt> with the same <tt>uid</tt> as <tt>vi</tt>. </li>
     *		 <li>if <tt>vi</tt> is a principal parameter, <tt>label</tt> ought to be
     *		 the bottom. </li>
     *	    </ul>
     *  <li>if <tt>vi</tt> is a <tt>PrincipalInstance</tt>, <tt>label</tt> ought to be
     *  the bottom. </li>
     *  </ul>
     */
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
	Context c = sc.context();
	JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
	JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();

	VarInstance vi = c.findVariable(name);
        if (!vi.isCanonical()) {
            // the instance is not yet ready
            return this;
        }
    
	if (vi instanceof JifVarInstance) {
	    JifVarInstance jvi = (JifVarInstance) vi;
	    return nf.CanonicalLabelNode(position(), jvi.label());
	}

	if (vi instanceof ParamInstance) {
	    ParamInstance pi = (ParamInstance) vi;

	    if (pi.isCovariantLabel()) {
		return nf.CanonicalLabelNode(position(),
			                     ts.covariantLabel(position(),
							       pi.uid()));
	    }
	    if (pi.isInvariantLabel()) {
		return nf.CanonicalLabelNode(position(),
			                     ts.paramLabel(position(),
							   pi.uid()).                       
                  description("label parameter " + pi.name() + 
                              " of class " + pi.container().fullName()));
	    }
	    if (pi.isPrincipal()) {
                throw new SemanticException("Cannot use the principal " + 
                     name + " as a label. (\"" + name + ":\" may have " +
                     "been intended.)", this.position());
	    }
	}

	if (vi instanceof PrincipalInstance) {
            throw new SemanticException("Cannot use the principal " + 
                 name + " as a label. (\"" + name + ":\" may have " +
                 "been intended.)", this.position());
	}

	throw new SemanticException(vi + " cannot be used as a label.", this.position());
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write(name);
    }
}
