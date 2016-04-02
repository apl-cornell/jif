package jif.ast;

import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.Ext;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class LabelActsForPrincipalConstraintNode_c
        extends ActsForConstraintNode_c<Label, Principal>
        implements LabelActsForPrincipalConstraintNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

//    @Deprecated
    public LabelActsForPrincipalConstraintNode_c(Position pos, LabelNode actor,
            PrincipalNode granter) {
        this(pos, actor, granter, null);
    }

    public LabelActsForPrincipalConstraintNode_c(Position pos, LabelNode actor,
            PrincipalNode granter, Ext ext) {
        super(pos, actor, granter, ext);
    }

}
