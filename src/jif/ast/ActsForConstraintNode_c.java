package jif.ast;

import jif.types.ActsForConstraint;
import jif.types.ActsForParam;
import jif.types.JifTypeSystem;
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
        extends ConstraintNode_c<ActsForConstraint<Actor, Granter>> implements
        ActsForConstraintNode<Actor, Granter> {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected ActsForParamNode<Actor> actor;
    protected ActsForParamNode<Granter> granter;
    protected final boolean isEquiv;

    public ActsForConstraintNode_c(Position pos, ActsForParamNode<Actor> actor,
            ActsForParamNode<Granter> granter, boolean isEquiv) {
        super(pos);
        this.actor = actor;
        this.granter = granter;
        this.isEquiv = isEquiv;
    }

    public ActsForConstraintNode_c(Position pos, ActsForParamNode<Actor> actor,
            ActsForParamNode<Granter> granter) {
        this(pos, actor, granter, false);
    }

    @Override
    public ActsForParamNode<Actor> actor() {
        return this.actor;
    }

    @Override
    public ActsForConstraintNode<Actor, Granter> actor(
            ActsForParamNode<Actor> actor) {
        ActsForConstraintNode_c<Actor, Granter> n = copy();
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
        ActsForConstraintNode_c<Actor, Granter> n = copy();
        n.granter = granter;
        if (constraint() != null) {
            n.setConstraint(constraint().granter(granter.parameter()));
        }
        return n;
    }

    protected ActsForConstraintNode_c<Actor, Granter> reconstruct(
            ActsForParamNode<Actor> actor, ActsForParamNode<Granter> granter) {
        if (actor != this.actor || granter != this.granter) {
            ActsForConstraintNode_c<Actor, Granter> n = copy();
            return (ActsForConstraintNode_c<Actor, Granter>) n.actor(actor)
                    .granter(granter);
        }

        return this;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        @SuppressWarnings("unchecked")
        ActsForParamNode<Actor> actor =
                (ActsForParamNode<Actor>) visitChild(this.actor, v);
        @SuppressWarnings("unchecked")
        ActsForParamNode<Granter> granter =
                (ActsForParamNode<Granter>) visitChild(this.granter, v);
        return reconstruct(actor, granter);
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
