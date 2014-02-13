/* new-begin */
package jif.ast;

import jif.types.RifTransition;
import jif.types.RifTransition_c;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

public class RifTransitionNode_c extends RifComponentNode_c implements
        RifTransitionNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected Id name;
    protected Id lstate;
    protected Id rstate;
    protected RifTransition transition;

    public RifTransitionNode_c(Position pos, Id name, Id lstate, Id rstate) {
        super(pos);
        this.name = name;
        this.lstate = lstate;
        this.rstate = rstate;
        this.transition = null;
    }

    @Override
    public Id name() {
        return this.name;
    }

    @Override
    public Id lstate() {
        return this.lstate;
    }

    @Override
    public Id rstate() {
        return this.rstate;
    }

    @Override
    public RifTransition transition() {
        return this.transition;
    }

    protected RifTransitionNode_c reconstruct(Id name, Id lstate, Id rstate) {
        if (name != this.name || lstate != this.lstate || rstate != this.rstate) {
            RifTransitionNode_c n = (RifTransitionNode_c) copy();
            n.name = name;
            n.lstate = lstate;
            n.rstate = rstate;
            return n;
        }

        return this;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        Id name = (Id) visitChild(this.name, v);
        Id lstate = (Id) visitChild(this.lstate, v);
        Id rstate = (Id) visitChild(this.rstate, v);
        return reconstruct(name, lstate, rstate);
    }

    @Override
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
        RifTransition t =
                new RifTransition_c(this.name, this.lstate, this.rstate);
        this.transition = t;
        return this;
    }

    @Override
    public boolean isDisambiguated() {
        return this.transition != null;
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        print(this.name, w, tr);
        w.write(":");
        w.allowBreak(0, " ");
        print(this.lstate, w, tr);
        w.write("->");
        print(this.rstate, w, tr);
    }

}

/* new-end */
