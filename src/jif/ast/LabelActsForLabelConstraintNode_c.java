package jif.ast;

import jif.types.label.Label;
import polyglot.ast.Ext;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class LabelActsForLabelConstraintNode_c
        extends ActsForConstraintNode_c<Label, Label>
        implements LabelActsForLabelConstraintNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

//    @Deprecated
    public LabelActsForLabelConstraintNode_c(Position pos, LabelNode actor,
            LabelNode granter) {
        this(pos, actor, granter, null);
    }

    public LabelActsForLabelConstraintNode_c(Position pos, LabelNode actor,
            LabelNode granter, Ext ext) {
        super(pos, actor, granter, ext);
    }
}
