package jif.ast;

import jif.types.label.Label;
import polyglot.util.Position;

public class LabelActsForLabelConstraintNode_c extends
        ActsForConstraintNode_c<Label, Label> implements
        LabelActsForLabelConstraintNode {

    public LabelActsForLabelConstraintNode_c(Position pos, LabelNode actor,
            LabelNode granter) {
        super(pos, actor, granter);
    }
}
