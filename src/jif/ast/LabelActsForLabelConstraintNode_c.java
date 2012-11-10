package jif.ast;

import jif.types.label.Label;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class LabelActsForLabelConstraintNode_c extends
        ActsForConstraintNode_c<Label, Label> implements
        LabelActsForLabelConstraintNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public LabelActsForLabelConstraintNode_c(Position pos, LabelNode actor,
            LabelNode granter) {
        super(pos, actor, granter);
    }
}
