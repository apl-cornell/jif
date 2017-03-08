package jif.ast;

import java.util.LinkedList;
import java.util.List;

import jif.types.JifPolyType;
import jif.types.JifTypeSystem;
import jif.types.Param;
import jif.types.ParamInstance;
import polyglot.ast.Expr;
import polyglot.ast.Ext;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.Node_c;
import polyglot.ast.Receiver;
import polyglot.ast.TypeNode;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;

/** An implementation of the <code>AmbParamTypeOrAccess</code> interface.
 */
public class AmbParamTypeOrAccess_c extends Node_c
        implements AmbParamTypeOrAccess {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected Receiver prefix;
    protected Object expr;
    protected Type type;

//    @Deprecated
    public AmbParamTypeOrAccess_c(Position pos, Receiver prefix, Object expr) {
        this(pos, prefix, expr, null);
    }

    public AmbParamTypeOrAccess_c(Position pos, Receiver prefix, Object expr,
            Ext ext) {
        super(pos, ext);
        this.prefix = prefix;
        this.expr = expr;
    }

    @Override
    public boolean isDisambiguated() {
        return false;
    }

    @Override
    public Receiver prefix() {
        return this.prefix;
    }

    public AmbParamTypeOrAccess prefix(Receiver prefix) {
        return prefix(this, prefix);
    }

    protected <N extends AmbParamTypeOrAccess_c> N prefix(N n,
            Receiver prefix) {
        if (n.prefix == prefix) return n;
        n = copyIfNeeded(n);
        n.prefix = prefix;
        return n;
    }

    @Override
    public Object expr() {
        return this.expr;
    }

    protected <N extends AmbParamTypeOrAccess_c> N expr(N n, Object expr) {
        if (n.expr == expr) return n;
        n = copyIfNeeded(n);
        n.expr = expr;
        return n;
    }

    @Override
    public Type type() {
        return this.type;
    }

    protected <N extends AmbParamTypeOrAccess_c> N reconstruct(N n,
            Receiver prefix, Object expr) {
        n = prefix(n, prefix);
        n = expr(n, expr);
        return n;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        Receiver prefix = visitChild(this.prefix, v);
        Object expr = this.expr;
        if (expr instanceof Expr) {
            expr = visitChild((Expr) expr, v);
        }
        return reconstruct(this, prefix, expr);
    }

    @Override
    public String toString() {
        return prefix + "[" + expr + "]{amb}";
    }

    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) ar.nodeFactory();

        if (!ar.isASTDisambiguated(prefix) || (expr instanceof Expr
                && !ar.isASTDisambiguated((Expr) expr))) {
            ar.job().extensionInfo().scheduler().currentGoal()
                    .setUnreachableThisRun();
            return this;
        }

        if (prefix instanceof TypeNode) {
            // "expr" must be a parameter.
            TypeNode tn = (TypeNode) prefix;

            if (!(tn.type() instanceof JifPolyType)) {
                throw new SemanticException(
                        tn.type() + " is not a parameterized type.",
                        position());
            }
            JifPolyType pt = (JifPolyType) tn.type();

            if (pt.params().isEmpty()) {
                throw new SemanticException(
                        tn.type() + " is not a parameterized type.",
                        position());
            }

            ParamNode n;
            ParamInstance pi = pt.params().get(0);
            if (expr instanceof Expr) {
                n = nf.AmbParam(position(), (Expr) expr, pi);
                n = (ParamNode) n.del().disambiguate(ar);
            } else {
                n = nf.AmbParam(position(), (Id) expr, pi);
                n = (ParamNode) n.del().disambiguate(ar);
                if (!n.isDisambiguated()) {
                    throw new SemanticException("\"" + expr + "\" is not "
                            + "suitable as a parameter.", position());

                }
            }

            List<Param> l = new LinkedList<Param>();
            l.add(n.parameter());

            Type t = ts.instantiate(position(), pt.instantiatedFrom(), l);

            return nf.CanonicalTypeNode(position(), t);
        } else if (prefix instanceof Expr) {
            // "expr" must be an access index.
            Expr n;
            if (expr instanceof Expr) {
                n = (Expr) expr;
            } else {
                n = nf.AmbExpr(position(), (Id) expr);
                ;
                n = (Expr) n.visit(ar);
            }
            return nf.ArrayAccess(position(), (Expr) prefix, n);
        }

        throw new SemanticException(
                "Could not disambiguate type or expression.", position());
    }
}
