package jif.lang;

import java.util.Set;

import jif.lang.PrincipalUtil.DelegationPair;

/**
 * A Policy is a component of a label, and is either an integrity policy or
 * a confidentiality policy. 
 *  
 */
public interface Policy {
    /**
     * Does this policy relabel to policy p? If this method returns true,
     * then all delegations that this result depend upon (i.e., DelegationPairs)
     * should be added to the set s. If this method returns false, then the
     * set is not altered at all.
     * @param p
     * @param dependencies
     * @return
     */
    boolean relabelsTo(Policy p, Set<DelegationPair> s);
}
