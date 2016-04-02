package jif.ast;

import jif.types.ActsForConstraint;
import jif.types.ActsForParam;
import jif.types.JifTypeSystem;
import polyglot.ast.Ext;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.Translator;

public abstract class ActsForConstraintNode_c<Actor extends ActsForParam, Granter extends ActsForParam>
        extends ConstraintNode_c<ActsForConstraint<Actor, Granter>>
        implements ActsForConstraintNode<Actor, Granter> {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected ActsForParamNode<Actor> actor;
    protected ActsForParamNode<Granter> granter;
    protected final boolean isEquiv;

    @Deprecated
    public ActsForConstraintNode_c(Position pos, ActsForParamNode<Actor> actor,
            ActsForParamNode<Granter> granter, boolean isEquiv) {
        this(pos, actor, granter, isEquiv, null);
    }

    public ActsForConstraintNode_c(Position pos, ActsForParamNode<Actor> actor,
            ActsForParamNode<Granter> granter, boolean isEquiv, Ext ext) {
        super(pos, ext);
        this.actor = actor;
        this.granter = granter;
        this.isEquiv = isEquiv;
    }

    @Deprecated
    public ActsForConstraintNode_c(Position pos, ActsForParamNode<Actor> actor,
            ActsForParamNode<Granter> granter) {
        this(pos, actor, granter, null);
    }

    public ActsForConstraintNode_c(Position pos, ActsForParamNode<Actor> actor,
            ActsForParamNode<Granter> granter, Ext ext) {
        this(pos, actor, granter, false, ext);
    }

    @Override
    public ActsForParamNode<Actor> actor() {
        return this.actor;
    }

    @Override
    public ActsForConstraintNode<Actor, Granter> actor(
            ActsForParamNode<Actor> actor) {
        return actor(this, actor);
    }

    protected <N extends ActsForConstraintNode_c<Actor, Granter>> N actor(N n,
            ActsForParamNode<Actor> actor) {
        if (n.actor == actor) return n;
        n = copyIfNeeded(n);
        n.actor = actor;
        if (constraint() != null) {
            n.setConstraint(constraint().actor(actor.parameter()));
        }
        return n;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ActsForConstraintNode_c<Actor, Granter> copy() {
        return (ActsForConstraintNode_c<Actor, Granter>) super.copy();
    }

    @Override
    public ActsForParamNode<Granter> granter() {
        return this.granter;
    }

    @Override
    public ActsForConstraintNode<Actor, Granter> granter(
            ActsForParamNode<Granter> granter) {
        return granter(this, granter);
    }

    protected <N extends ActsForConstraintNode_c<Actor, Granter>> N granter(N n,
            ActsForParamNode<Granter> granter) {
        if (n.granter == granter) return n;
        n = copyIfNeeded(n);
        n.granter = granter;
        if (constraint() != null) {
            n.setConstraint(constraint().granter(granter.parameter()));
        }
        return n;
    }

    protected <N extends ActsForConstraintNode_c<Actor, Granter>> N reconstruct(
            N n, ActsForParamNode<Actor> actor,
            ActsForParamNode<Granter> granter) {
        n = actor(n, actor);
        n = granter(n, granter);
        return n;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        ActsForParamNode<Actor> actor = visitChild(this.actor, v);
        ActsForParamNode<Granter> granter = visitChild(this.granter, v);
        return reconstruct(this, actor, granter);
    }

    /**
     * @throws SemanticException  
     */
    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        if (constraint() == null && actor.isDisambiguated()
                && granter.isDisambiguated()) {
            JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
            return constraint(ts.actsForConstraint(position, actor.parameter(),
                    granter.parameter(), isEquiv));
        }

        return this;
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter pp) {
        print(actor, w, pp);
        w.write(" ");
        w.write(isEquiv ? "equiv" : "actsfor");
        w.write(" ");
        print(granter, w, pp);
    }

    @Override
    public void translate(CodeWriter w, Translator tr) {
        print(actor, w, tr);
        w.write(" ");
        w.write(isEquiv ? "equiv" : "actsfor");
        w.write(" ");
        print(granter, w, tr);
    }

}
