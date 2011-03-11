package jif.ast;

import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.util.Position;

public class LabelActsForPrincipalConstraintNode_c extends
        ActsForConstraintNode_c<Label, Principal> implements
        LabelActsForPrincipalConstraintNode {

    public LabelActsForPrincipalConstraintNode_c(Position pos, LabelNode actor,
            PrincipalNode granter) {
        super(pos, actor, granter);
    }

}
