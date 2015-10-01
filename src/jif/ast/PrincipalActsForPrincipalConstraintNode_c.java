package jif.ast;

import jif.types.principal.Principal;
import polyglot.ast.Ext;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/**
 * An implementation of the <tt>PrincipalActsForPrincipalConstraintNode</tt>
 * interface.
 */
public class PrincipalActsForPrincipalConstraintNode_c
        extends ActsForConstraintNode_c<Principal, Principal>
        implements PrincipalActsForPrincipalConstraintNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

//    @Deprecated
    public PrincipalActsForPrincipalConstraintNode_c(Position pos,
            PrincipalNode actor, PrincipalNode granter, boolean isEquiv) {
        this(pos, actor, granter, isEquiv, null);
    }

    public PrincipalActsForPrincipalConstraintNode_c(Position pos,
            PrincipalNode actor, PrincipalNode granter, boolean isEquiv,
            Ext ext) {
        super(pos, actor, granter, isEquiv, ext);
    }
}
