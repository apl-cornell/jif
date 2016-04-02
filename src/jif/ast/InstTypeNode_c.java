package jif.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jif.types.JifPolyType;
import jif.types.JifTypeSystem;
import jif.types.Param;
import jif.types.ParamInstance;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.Ambiguous;
import polyglot.ast.Ext;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ast.TypeNode_c;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.InternalCompilerError;
import polyglot.util.ListUtil;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.ExceptionChecker;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;
import polyglot.visit.TypeChecker;

/** An implementation of the <code>InstTypeNode</code> interface.
 */
public class InstTypeNode_c extends TypeNode_c
        implements InstTypeNode, Ambiguous {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected TypeNode base;
    protected List<ParamNode> params;

//    @Deprecated
    public InstTypeNode_c(Position pos, TypeNode base, List<ParamNode> params) {
        this(pos, base, params, null);
    }

    public InstTypeNode_c(Position pos, TypeNode base, List<ParamNode> params,
            Ext ext) {
        super(pos, ext);
        this.base = base;
        this.params = ListUtil.copy(params, true);
    }

    @Override
    public TypeNode base() {
        return this.base;
    }

    @Override
    public InstTypeNode base(TypeNode base) {
        InstTypeNode_c n = (InstTypeNode_c) copy();
        n.base = base;
        return n;
    }

    @Override
    public List<ParamNode> params() {
        return this.params;
    }

    @Override
    public InstTypeNode params(List<ParamNode> params) {
        InstTypeNode_c n = (InstTypeNode_c) copy();
        n.params = ListUtil.copy(params, true);
        return n;
    }

    protected InstTypeNode_c reconstruct(TypeNode base,
            List<ParamNode> params) {
        if (base != this.base || !CollectionUtil.equals(params, this.params)) {
            InstTypeNode_c n = (InstTypeNode_c) copy();
            n.base = base;
            n.params = ListUtil.copy(params, true);
            return n;
        }

        return this;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        TypeNode base = visitChild(this.base, v);
        Type b = base.type();

        List<ParamNode> newParams = this.params;
        if (b != null && b.isCanonical() && b instanceof JifPolyType) {
            JifPolyType t = (JifPolyType) b;
            newParams = new ArrayList<ParamNode>(this.params.size());

            Iterator<ParamInstance> i = t.params().iterator();
            Iterator<ParamNode> j = this.params.iterator();
            ParamInstance pi = null;
            while (j.hasNext()) {
                if (i.hasNext()) pi = i.next();
                ParamNode p = j.next();
                if (p instanceof AmbExprParam) {
                    p = ((AmbExprParam) p).expectedPI(pi);
                }
                newParams.add(p);
            }
        }

        List<ParamNode> params = visitList(newParams, v);
        return reconstruct(base, params);
    }

    @Override
    public boolean isDisambiguated() {
        return false;
    }

    @Override
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
        Type b = base.type();

        if (!base.isDisambiguated() || !b.isCanonical()) {
            //  not yet ready to disambiguate
            sc.job().extensionInfo().scheduler().currentGoal()
                    .setUnreachableThisRun();
            return this;
        }

        if (!(b instanceof JifPolyType)
                || ((JifPolyType) b).params().isEmpty()) {
            throw new SemanticException(
                    "Cannot instantiate from a non-polymorphic type " + b);
        }
        JifPolyType t = (JifPolyType) b;

        List<Param> l = new LinkedList<Param>();

        Iterator<ParamNode> i = this.params.iterator();
        Iterator<ParamInstance> j = t.params().iterator();
        while (i.hasNext() && j.hasNext()) {
            ParamNode p = i.next();
            ParamInstance pi = j.next();

            if (!p.isDisambiguated()) {
                // the param is not yet ready
                sc.job().extensionInfo().scheduler().currentGoal()
                        .setUnreachableThisRun();
                return this;
            }

            checkParamSuitable(pi, p);

            l.add(p.parameter());
        }
        if (i.hasNext()) {
            throw new SemanticException(
                    "Too many parameters supplied for the " + "class " + t,
                    this.position());
        }

        return sc.nodeFactory().CanonicalTypeNode(position(),
                ts.instantiate(position(), t.instantiatedFrom(), l));
    }

    protected void checkParamSuitable(ParamInstance pi, ParamNode p)
            throws SemanticException {
        if (pi.isLabel() && !(p.parameter() instanceof Label)) {
            throw new SemanticException(
                    "Can not instantiate a "
                            + "label parameter with a non-label.",
                    p.position());
        }
        if (pi.isPrincipal() && !(p.parameter() instanceof Principal)) {
            throw new SemanticException(
                    "Can not instantiate a "
                            + "principal parameter with a non-principal.",
                    p.position());
        }
        if (pi.isInvariantLabel() && !((Label) p.parameter()).isInvariant())
            throw new SemanticException(
                    "Can not instantiate an invariant "
                            + "label parameter with a non-invariant label.",
                    p.position());
    }

    @Override
    public Node typeCheck(TypeChecker tc) {
        throw new InternalCompilerError(position(),
                "Cannot type check ambiguous node " + this + ".");
    }

    @Override
    public Node exceptionCheck(ExceptionChecker ec) {
        throw new InternalCompilerError(position(),
                "Cannot exception check ambiguous node " + this + ".");
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        print(base, w, tr);
        w.write("[");

        for (Iterator<ParamNode> i = params.iterator(); i.hasNext();) {
            ParamNode p = i.next();
            print(p, w, tr);
            if (i.hasNext()) {
                w.write(",");
                w.allowBreak(0, " ");
            }
        }

        w.write("]");
    }

    @Override
    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError(position(),
                "Cannot translate ambiguous node " + this + ".");
    }

    @Override
    public String toString() {
        return base + "[...]";
    }
}
