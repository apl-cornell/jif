package jif.ast;

import java.util.LinkedList;
import java.util.List;

import jif.types.JifPolyType;
import jif.types.JifTypeSystem;
import polyglot.ast.*;
import polyglot.ext.jl.ast.Node_c;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;

/** An implementation of the <code>AmbParamTypeOrAccess</code> interface.
 */
public class AmbParamTypeOrAccess_c extends Node_c implements AmbParamTypeOrAccess
{
    protected Receiver prefix;
    protected String name;
    protected Type type;

    public AmbParamTypeOrAccess_c(Position pos, Receiver prefix, String name) {
	super(pos);
	this.prefix = prefix;
	this.name = name;
    }

    public boolean isDisambiguated() {
        return false;
    }

    public Receiver prefix() {
	return this.prefix;
    }

    public AmbParamTypeOrAccess prefix(Receiver prefix) {
	AmbParamTypeOrAccess_c n = (AmbParamTypeOrAccess_c) copy();
	n.prefix = prefix;
	return n;
    }

    public String name() {
	return this.name;
    }

    public AmbParamTypeOrAccess name(String name) {
	AmbParamTypeOrAccess_c n = (AmbParamTypeOrAccess_c) copy();
	n.name = name;
	return n;
    }

    public Type type() {
        return this.type;
    }

    protected AmbParamTypeOrAccess_c reconstruct(Receiver prefix) {
	if (prefix != this.prefix) {
	    AmbParamTypeOrAccess_c n = (AmbParamTypeOrAccess_c) copy();
	    n.prefix = prefix;
	    return n;
	}

	return this;
    }

    public Node visitChildren(NodeVisitor v) {
	Receiver prefix = (Receiver) visitChild(this.prefix, v);
	return reconstruct(prefix);
    }

    public String toString() {
	return prefix + "[" + name + "]{amb}";
    }

    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
	JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
	JifNodeFactory nf = (JifNodeFactory) ar.nodeFactory();

	if (!prefix.isDisambiguated()) return this;
    
	if (prefix instanceof TypeNode) {
	    // "name" must be a parameter.
	    TypeNode tn = (TypeNode) prefix;

	    if (! (tn.type() instanceof JifPolyType)) {
		throw new SemanticException(tn.type() + " is not a parameterized type.", position());
	    }

	    ParamNode n = nf.AmbParam(position(), name);
	    n = (ParamNode) n.visit(ar);

	    List l = new LinkedList();
            l.add(n.parameter());

	    Type t = ts.instantiate(position(),
                                    ((JifPolyType) tn.type()).instantiatedFrom(), l);

            return nf.CanonicalTypeNode(position(), t);
	}
	else if (prefix instanceof Expr) {
	    // "name" must be an expression.
	    Expr n = nf.AmbExpr(position(), name);
	    n = (Expr) n.visit(ar);
	    return nf.ArrayAccess(position(), (Expr) prefix, n);
	}

	throw new SemanticException("Could not disambiguate type or expression.", position());
    }
}
