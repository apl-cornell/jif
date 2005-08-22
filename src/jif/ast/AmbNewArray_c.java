package jif.ast;

import java.util.*;

import jif.types.*;
import polyglot.ast.*;
import polyglot.ext.jl.ast.Expr_c;
import polyglot.frontend.MissingDependencyException;
import polyglot.frontend.Scheduler;
import polyglot.frontend.goals.Goal;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.*;
import polyglot.visit.*;

/** An implementation of the <code>AmbNewArray</code> interface.
 */
public class AmbNewArray_c extends Expr_c implements AmbNewArray
{
    protected TypeNode baseType;
    /** The ambiguous expr. May be a parameter or an array dimension. */
    protected Object expr;
    protected List dims;

    public AmbNewArray_c(Position pos, TypeNode baseType, Object expr, List dims) {
	super(pos);
	this.baseType = baseType;
	this.expr = expr;
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

    /** Gets the expr. */
    public Object expr() {
	return this.expr;
    }

    /** Returns a copy of this node with <code>name</code> updated. */
//    public AmbNewArray expr(Expr expr) {
//	AmbNewArray_c n = (AmbNewArray_c) copy();
//	n.expr = expr;
//	return n;
//    }

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
    protected AmbNewArray_c reconstruct(TypeNode baseType, Object expr, List dims) {
	if (baseType != this.baseType || expr != this.expr || ! CollectionUtil.equals(dims, this.dims)) {
	    AmbNewArray_c n = (AmbNewArray_c) copy();
	    n.baseType = baseType;
	    n.expr = expr;
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
	Object expr = this.expr;
	if (expr instanceof Expr) {
	    expr = visitChild((Expr)expr, v);
	}
	return reconstruct(baseType, expr, dims);
    }

    public String toString() {
	return "new " + baseType + "[" + expr + "]...{amb}";
    }

    /** Disambiguates
     */
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
	if (expr instanceof Expr && !ar.isASTDisambiguated((Expr)expr)) {
            Scheduler sched = ar.job().extensionInfo().scheduler();
            Goal g = sched.Disambiguated(ar.job());
            throw new MissingDependencyException(g);
	}

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
		throw new SemanticDetailedException(
		          "Not enough parameters for parameterized type " + pt + ".",
		          "The type " + pt + " is a parameterized type with " +
		          pt.params().size() + " parameters. So, to instantiate this type, " +
		          "you must supply " + pt.params().size() + "",
		          this.position());
	    }
	    else if (pt.params().size() == 1) {
		// "name" is a parameter.  Instantiate the base type with the
		// parameter and use it as the new base type.
                ParamNode pn;
                ParamInstance pi = (ParamInstance)pt.params().get(0);
                if (expr instanceof Expr) {
                    pn = nf.AmbParam(position(), (Expr)expr, pi);                    
                }
                else {
                    pn = nf.AmbParam(position(), (String)expr, pi);                                        
                }

                pn = (ParamNode) pn.disambiguate(ar);

		List l = new LinkedList();
                if (!pn.isDisambiguated()) {
                    // the instance is not yet ready
                    Goal g = ar.job().extensionInfo().scheduler().Disambiguated(ar.job());
                    throw new MissingDependencyException(g);
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
	Expr e;
	if (expr instanceof Expr) {
	    e = (Expr) ((Expr)expr).visit(ar);
	}
	else {
	    e = nf.AmbExpr(position(), (String)expr);
	    e = (Expr) e.visit(ar);
	}


	List l = new LinkedList();
	l.add(e);
	l.addAll(dims);

	return nf.NewArray(position(), baseType, dims);
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("new ");
        print(baseType, w, tr);
        w.write("[");
        if (expr instanceof Expr) {
            print((Expr)expr, w, tr);
        }
        else {
            w.write((String)expr);            
        }
        w.write("]");

        for (Iterator i = dims.iterator(); i.hasNext();) {
            Expr e = (Expr) i.next();
            w.write("[");
            printBlock(e, w, tr);
            w.write("]");
        }
    }
}
