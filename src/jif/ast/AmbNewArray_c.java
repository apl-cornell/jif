package jif.ast;

import java.util.LinkedList;
import java.util.List;

import jif.types.JifPolyType;
import jif.types.JifTypeSystem;
import jif.types.Param;
import jif.types.ParamInstance;
import jif.types.SemanticDetailedException;
import polyglot.ast.Expr;
import polyglot.ast.Expr_c;
import polyglot.ast.Ext;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.ast.TypeNode;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.InternalCompilerError;
import polyglot.util.ListUtil;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.CFGBuilder;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

/** An implementation of the <code>AmbNewArray</code> interface.
 */
public class AmbNewArray_c extends Expr_c implements AmbNewArray {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected TypeNode baseType;
    /** The ambiguous expr. May be a parameter or an array dimension. */
    protected Object expr;
    protected List<Expr> dims;
    protected int addDims;

//    @Deprecated
    public AmbNewArray_c(Position pos, TypeNode baseType, Object expr,
            List<Expr> dims, int addDims) {
        this(pos, baseType, expr, dims, addDims, null);
    }

    public AmbNewArray_c(Position pos, TypeNode baseType, Object expr,
            List<Expr> dims, int addDims, Ext ext) {
        super(pos, ext);
        this.baseType = baseType;
        this.expr = expr;
        if (!(expr instanceof Expr) && !(expr instanceof Id)) {
            throw new InternalCompilerError(
                    "wrong type for expr: " + expr.getClass().getName());
        }
        this.dims = ListUtil.copy(dims, true);
        this.addDims = addDims;
    }

    @Override
    public boolean isDisambiguated() {
        return false;
    }

    /** Gets the base type.     */
    @Override
    public TypeNode baseType() {
        return this.baseType;
    }

    /** Returns a copy of this node with <code>baseType</code> updated. */
    @Override
    public AmbNewArray baseType(TypeNode baseType) {
        return baseType(this, baseType);
    }

    protected <N extends AmbNewArray_c> N baseType(N n, TypeNode baseType) {
        if (n.baseType == baseType) return n;
        n = copyIfNeeded(n);
        n.baseType = baseType;
        return n;
    }

    /** Gets the expr. */
    @Override
    public Object expr() {
        return this.expr;
    }

    protected <N extends AmbNewArray_c> N expr(N n, Object expr) {
        if (n.expr == expr) return n;
        n = copyIfNeeded(n);
        n.expr = expr;
        return n;
    }

    /** Gets the additional dimensions. */
    @Override
    public List<? extends Expr> dims() {
        return this.dims;
    }

    /** Returns a copy of this node with <code>dims</code> updated. */
    @Override
    public AmbNewArray dims(List<? extends Expr> dims) {
        return dims(this, dims);
    }

    protected <N extends AmbNewArray_c> N dims(N n, List<? extends Expr> dims) {
        if (CollectionUtil.equals(n.dims, dims)) return n;
        n = copyIfNeeded(n);
        n.dims = ListUtil.copy(dims, true);
        return n;
    }

    @Override
    public int additionalDims() {
        return this.addDims;
    }

    @Override
    public AmbNewArray additionalDims(int addDims) {
        AmbNewArray_c n = (AmbNewArray_c) copy();
        n.addDims = addDims;
        return n;
    }

    /** Reconstructs the node. */
    protected <N extends AmbNewArray_c> N reconstruct(N n, TypeNode baseType,
            Object expr, List<? extends Expr> dims) {
        n = baseType(n, baseType);
        n = expr(n, expr);
        n = dims(n, dims);
        return n;
    }

    /**
     * Visit this term in evaluation order.
     */
    @Override
    public <T> List<T> acceptCFG(CFGBuilder<?> v, List<T> succs) {
        return succs;
    }

    @Override
    public Term firstChild() {
        return null;
    }

    /** Visits the children of this node. */
    @Override
    public Node visitChildren(NodeVisitor v) {
        TypeNode baseType = visitChild(this.baseType, v);
        List<? extends Expr> dims = visitList(this.dims, v);
        Object expr = this.expr;
        if (expr instanceof Expr) {
            expr = visitChild((Expr) expr, v);
        }
        return reconstruct(this, baseType, expr, dims);
    }

    @Override
    public String toString() {
        return "new " + baseType + "[" + expr + "]...{amb}";
    }

    /** Disambiguates
     */
    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        if (expr instanceof Expr && !ar.isASTDisambiguated((Expr) expr)) {
            ar.job().extensionInfo().scheduler().currentGoal()
                    .setUnreachableThisRun();
            return this;
        }

        JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) ar.nodeFactory();

        if (dims.isEmpty()) {
            throw new InternalCompilerError(position(),
                    "Cannot disambiguate ambiguous new array with no "
                            + "dimension expressions.");
        }

        Type t = baseType.type();

        if (t instanceof JifPolyType) {
            JifPolyType pt = (JifPolyType) t;

            if (pt.params().size() > 1) {
                //this node shouldn't be ambiguous.
                throw new SemanticDetailedException(
                        "Not enough parameters for parameterized type " + pt
                                + ".",
                        "The type " + pt + " is a parameterized type with "
                                + pt.params().size()
                                + " parameters. So, to instantiate this type, "
                                + "you must supply " + pt.params().size() + "",
                        this.position());
            } else if (pt.params().size() == 1) {
                // "name" is a parameter.  Instantiate the base type with the
                // parameter and use it as the new base type.
                ParamNode pn;
                ParamInstance pi = pt.params().get(0);
                if (expr instanceof Expr) {
                    pn = nf.AmbParam(position(), (Expr) expr, pi);
                } else {
                    pn = nf.AmbParam(position(), (Id) expr, pi);
                }

                pn = (ParamNode) pn.del().disambiguate(ar);

                List<Param> l = new LinkedList<Param>();
                if (!pn.isDisambiguated()) {
                    // the instance is not yet ready
                    ar.job().extensionInfo().scheduler().currentGoal()
                            .setUnreachableThisRun();
                    return this;
                }

                l.add(pn.parameter());

                Type base = ts.instantiate(baseType.position(),
                        pt.instantiatedFrom(), l);

                return nf.NewArray(position(),
                        nf.CanonicalTypeNode(baseType.position(), base), dims,
                        addDims);
            }
        }

        // "name" is an expression.  Prepend it to the list of dimensions.
        Expr e;
        if (expr instanceof Expr) {
            e = (Expr) ((Expr) expr).visit(ar);
        } else {
            e = nf.AmbExpr(position(), (Id) expr);
            e = (Expr) e.visit(ar);
        }

        List<Expr> l = new LinkedList<Expr>();
        l.add(e);
        l.addAll(dims);

        return nf.NewArray(position(), baseType, l, addDims);
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("new ");
        print(baseType, w, tr);
        w.write("[");
        if (expr instanceof Expr) {
            print((Expr) expr, w, tr);
        } else {
            w.write(((Id) expr).id());
        }
        w.write("]");

        for (Expr e : dims) {
            w.write("[");
            printBlock(e, w, tr);
            w.write("]");
        }
    }
}
