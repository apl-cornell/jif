package jif.ast;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jif.types.JifPolyType;
import jif.types.JifTypeSystem;
import polyglot.ast.*;
import polyglot.ext.jl.ast.Expr_c;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.*;
import polyglot.visit.*;

/** An implementation of the <code>AmbNewArray</code> interface.
 */
public class AmbNewArray_c extends Expr_c implements AmbNewArray
{
    protected TypeNode baseType;
    /** The ambiguous name, which may be an expression or a parameter. */
    protected String name;
    protected List dims;

    public AmbNewArray_c(Position pos, TypeNode baseType, String name, List dims) {
	super(pos);
	this.baseType = baseType;
	this.name = name;
	this.dims = TypedList.copyAndCheck(dims, Expr.class, true);
    }

    public boolean isDisambiguated() {
        return false;
    }

    /** Gets the base type.     */
    public TypeNode baseType() {
	return this.baseType;
    }

    /** Returns a copy of this node with <code>baseType</code> updated. */
    public AmbNewArray baseType(TypeNode baseType) {
	AmbNewArray_c n = (AmbNewArray_c) copy();
	n.baseType = baseType;
	return n;
    }

    /** Gets the ambiguous name. */
    public String name() {
	return this.name;
    }

    /** Returns a copy of this node with <code>name</code> updated. */
    public AmbNewArray name(String name) {
	AmbNewArray_c n = (AmbNewArray_c) copy();
	n.name = name;
	return n;
    }

    /** Gets the addtional dimensions. */
    public List dims() {
	return this.dims;
    }

    /** Returns a copy of this node with <code>dims</code> updated. */
    public AmbNewArray dims(List dims) {
	AmbNewArray_c n = (AmbNewArray_c) copy();
	n.dims = TypedList.copyAndCheck(dims, Expr.class, true);
	return n;
    }

    /** Reconstructs the node. */
    protected AmbNewArray_c reconstruct(TypeNode baseType, List dims) {
	if (baseType != this.baseType || ! CollectionUtil.equals(dims, this.dims)) {
	    AmbNewArray_c n = (AmbNewArray_c) copy();
	    n.baseType = baseType;
	    n.dims = TypedList.copyAndCheck(dims, Expr.class, true);
	    return n;
	}

	return this;
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

    /** Visits the children of this node. */
    public Node visitChildren(NodeVisitor v) {
	TypeNode baseType = (TypeNode) visitChild(this.baseType, v);
	List dims = visitList(this.dims, v);
	return reconstruct(baseType, dims);
    }

    public String toString() {
	return "new " + baseType + "[" + name + "]...{amb}";
    }

    /** Disambiguates <code>name</code>.
     */
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
	JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
	JifNodeFactory nf = (JifNodeFactory) ar.nodeFactory();

	if (dims.isEmpty()) {
	    throw new InternalCompilerError(position(),
		"Cannot disambiguate ambiguous new array with no " +
		"dimension expressions.");
	}

	Type t = baseType.type();

	if (t instanceof JifPolyType) {
	    JifPolyType pt = (JifPolyType) t;

	    if (pt.params().size() > 1) {
		//this node shouldn't be ambiguous.
		throw new SemanticException("Not enough parameters for " +
		    "parameterized types " + pt + ".");
	    }
	    else if (pt.params().size() == 1) {
		// "name" is a parameter.  Instantiate the base type with the
		// parameter and use it as the new base type.
                ParamNode pn = nf.AmbParam(position(), name);

                try {
                    pn = (ParamNode) pn.disambiguate(ar);
                }
                catch (SemanticException e) {
		    throw new SemanticException("cannot resolve the parameter: " + name, position());
                }

		List l = new LinkedList();
                if (!pn.isDisambiguated()) {
                    // the instance is not yet ready
                    // @@@@@is this right?
                    return this;
                }

            l.add(pn.parameter());

		Type base = ts.instantiate(baseType.position(),
                                           pt.instantiatedFrom(), l);

		return nf.NewArray(position(),
			           nf.CanonicalTypeNode(baseType.position(),
				                        base),
				   dims);
	    }
	}

	// "name" is an expression.  Prepend it to the list of dimensions.
	Expr e = nf.AmbExpr(position(), name);
	e = (Expr) e.visit(ar);

	List l = new LinkedList();
	l.add(e);
	l.addAll(dims);

	return nf.NewArray(position(), baseType, dims);
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("new ");
        print(baseType, w, tr);
        w.write("[");
        w.write(name);
        w.write("]");

        for (Iterator i = dims.iterator(); i.hasNext();) {
            Expr e = (Expr) i.next();
            w.write("[");
            printBlock(e, w, tr);
            w.write("]");
        }
    }
}
