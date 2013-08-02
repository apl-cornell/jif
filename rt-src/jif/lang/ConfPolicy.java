package jif.lang;

import java.util.Set;

import jif.lang.PrincipalUtil.DelegationPair;

public interface ConfPolicy extends Policy {
    /**
     * Return the join of this policy and p. The set s contains all
     * delegations (i.e., DelegationPairs) that this join result depends upon.
     */
    ConfPolicy join(ConfPolicy p, Set<DelegationPair> dependencies);

    ConfPolicy meet(ConfPolicy p, Set<DelegationPair> dependencies);

    ConfPolicy join(ConfPolicy p);

    ConfPolicy meet(ConfPolicy p);

    ConfPolicy join(ConfPolicy p, boolean simplify);

    ConfPolicy meet(ConfPolicy p, boolean simplify);

    ConfPolicy join(ConfPolicy p, Set<DelegationPair> dependencies,
            boolean simplify);

    ConfPolicy meet(ConfPolicy p, Set<DelegationPair> dependencies,
            boolean simplify);
}
