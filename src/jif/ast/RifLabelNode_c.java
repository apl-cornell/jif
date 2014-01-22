/* new-begin */
package jif.ast;

import java.util.List;

import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class RifLabelNode_c extends LabelNode_c implements RifLabelNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private List<List<RifComponentNode>> components;

    public RifLabelNode_c(Position pos, List<List<RifComponentNode>> components) {
        super(pos);
        this.components = components;
    }

    @Override
    public List<List<RifComponentNode>> components() {
        return this.components;
    }

}

/* new-end */
