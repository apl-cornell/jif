/* new-begin */
package jif.ast;

import polyglot.ast.Id;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class RifTransitionNode_c extends RifComponentNode_c implements RifTransitionNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private Id name;
    private Id lstate;
    private Id rstate;

    public RifTransitionNode_c(Position pos, Id name, Id lstate, Id rstate) {
        super(pos);
        this.name = name;
        this.lstate = lstate;
        this.lstate = rstate;
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
}

/* new-end */
