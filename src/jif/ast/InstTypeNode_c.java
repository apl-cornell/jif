package jif.ast;

import java.util.*;

import jif.types.*;
import jif.types.label.Label;
import polyglot.ast.*;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ext.jl.ast.TypeNode_c;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.*;
import polyglot.visit.*;

/** An implementation of the <code>InstTypeNode</code> interface.
 */
public class InstTypeNode_c extends TypeNode_c implements InstTypeNode, Ambiguous
{
    protected TypeNode base;
    protected List params;

    public InstTypeNode_c(Position pos, TypeNode base, List params) {
	super(pos);
	this.base = base;
	this.params = TypedList.copyAndCheck(params, ParamNode.class, true);
    }

    public TypeNode base() {
	return this.base;
    }

    public InstTypeNode base(TypeNode base) {
	InstTypeNode_c n = (InstTypeNode_c) copy();
	n.base = base;
	return n;
    }

    public List params() {
	return this.params;
    }

    public InstTypeNode params(List params) {
	InstTypeNode_c n = (InstTypeNode_c) copy();
	n.params = TypedList.copyAndCheck(params, ParamNode.class, true);
	return n;
    }

    protected InstTypeNode_c reconstruct(TypeNode base, List params) {
	if (base != this.base || ! CollectionUtil.equals(params, this.params)) {
	    InstTypeNode_c n = (InstTypeNode_c) copy();
	    n.base = base;
	    n.params = TypedList.copyAndCheck(params, ParamNode.class, true);
	    return n;
	}

	return this;
    }

    public Node visitChildren(NodeVisitor v) {
	TypeNode base = (TypeNode) visitChild(this.base, v);
	List params = visitList(this.params, v);
	return reconstruct(base, params);
    }

    public boolean isDisambiguated() {
        return false;
    }
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
	Type b = base.type();

	if (! b.isCanonical()) {
	    //  not yet ready to disambiguate
	    return this;
	}

        if (! (b instanceof JifPolyType)) {
	    throw new SemanticException(
		"Cannot instantiate from a non-polymorphic type " + b);
	}

	JifPolyType t = (JifPolyType) b;

	List l = new LinkedList();
	
	Iterator i = this.params.iterator();
	Iterator j = t.params().iterator();
	while (i.hasNext() && j.hasNext()) {
	    ParamNode p = (ParamNode) i.next();
	    ParamInstance pi = (ParamInstance) j.next();

	    if (pi.isInvariantLabel() && !((Label)p.parameter()).isInvariant() ) 
		throw new SemanticException("Can not instantiate an invariant "+
			"label parameter with a non-invariant label.", 
			position());

	    l.add(p.parameter());
	}

	return sc.nodeFactory().CanonicalTypeNode(position(),
		ts.instantiate(position(), t.instantiatedFrom(), l) );
    }

    public Node typeCheck(TypeChecker tc) throws SemanticException {
	throw new InternalCompilerError(position(),
	    "Cannot type check ambiguous node " + this + ".");
    }

    public Node exceptionCheck(ExceptionChecker ec) throws SemanticException {
	throw new InternalCompilerError(position(),
	    "Cannot exception check ambiguous node " + this + ".");
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        print(base, w, tr);
        w.write("[");

        for (Iterator i = params.iterator(); i.hasNext(); ) {
	    ParamNode p = (ParamNode) i.next();
            print(p, w, tr);
            if (i.hasNext()) {
                w.write(",");
                w.allowBreak(0, " ");
            }
        }

        w.write("]");
    }

    public void translate(CodeWriter w, Translator tr) {
	throw new InternalCompilerError(position(),
	    "Cannot translate ambiguous node " + this + ".");
    }

    public String toString() {
	return base + "[...]";
    }
}
