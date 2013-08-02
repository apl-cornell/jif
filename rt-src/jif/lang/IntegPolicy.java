package jif.lang;

import java.util.Set;

import jif.lang.PrincipalUtil.DelegationPair;

public interface IntegPolicy extends Policy {
    /**
     * Return the join of this policy and p. The set s contains all
     * delegations (i.e., DelegationPairs) that this join result depends upon.
     */
    IntegPolicy join(IntegPolicy p, Set<DelegationPair> dependencies);

    IntegPolicy meet(IntegPolicy p, Set<DelegationPair> dependencies);

    IntegPolicy join(IntegPolicy p);

    IntegPolicy meet(IntegPolicy p);

    IntegPolicy join(IntegPolicy p, boolean simplify);

    IntegPolicy meet(IntegPolicy p, boolean simplify);

    IntegPolicy join(IntegPolicy p, Set<DelegationPair> dependencies,
            boolean simplify);

    IntegPolicy meet(IntegPolicy p, Set<DelegationPair> dependencies,
            boolean simplify);
}
