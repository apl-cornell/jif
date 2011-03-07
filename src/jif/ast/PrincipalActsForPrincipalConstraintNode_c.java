package jif.ast;

import jif.types.principal.Principal;
import polyglot.util.Position;

/**
 * An implementation of the <tt>PrincipalActsForPrincipalConstraintNode</tt>
 * interface.
 */
public class PrincipalActsForPrincipalConstraintNode_c extends
        ActsForConstraintNode_c<Principal, Principal> implements
        PrincipalActsForPrincipalConstraintNode {

    public PrincipalActsForPrincipalConstraintNode_c(Position pos,
            PrincipalNode actor, PrincipalNode granter, boolean isEquiv) {
        super(pos, actor, granter, isEquiv);
    }
}
