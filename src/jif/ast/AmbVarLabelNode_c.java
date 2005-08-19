package jif.ast;

import jif.types.*;
import jif.types.label.ParamLabel;
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

    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
	Context c = sc.context();
	JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
	JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();

	VarInstance vi = c.findVariable(name);
    
	if (vi instanceof JifVarInstance) {
	    JifVarInstance jvi = (JifVarInstance) vi;
	    return nf.CanonicalLabelNode(position(), jvi.label());
	}

	if (vi instanceof ParamInstance) {
	    ParamInstance pi = (ParamInstance) vi;

	    if (pi.isCovariantLabel()) {
		return nf.CanonicalLabelNode(position(),
			                     ts.covariantLabel(position(), pi));
	    }
	    if (pi.isInvariantLabel()) {
                ParamLabel pl = ts.paramLabel(position(), pi);
                pl.setDescription("label parameter " + pi.name() + 
                                  " of class " + pi.container().fullName());
                
		return nf.CanonicalLabelNode(position(), pl);        
	    }
	    if (pi.isPrincipal()) {
                throw new SemanticException("Cannot use the external principal " + 
                     name + " as a label. (The label \"{" + name + ": }\" may have " +
                     "been intended.)", this.position());
	    }
	}

	if (vi instanceof PrincipalInstance) {
            throw new SemanticException("Cannot use the external principal " + 
                 name + " as a label. (The label \"{" + name + ": }\" may have " +
                 "been intended.)", this.position());
	}

	throw new SemanticException(vi + " cannot be used as a label.", this.position());
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write(name);
    }
}
