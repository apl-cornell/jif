package jif.lang;

import java.util.Set;

import jif.lang.PrincipalUtil.DelegationPair;

/**
 * Represents the join of integrity policies
 * 
 */
public final class JoinIntegPolicy extends JoinPolicy implements IntegPolicy {
    JoinIntegPolicy(LabelUtil labelUtil, Set<Policy> policies) {
        super(labelUtil, policies);
    }

    @Override
    public IntegPolicy join(IntegPolicy p, Set<DelegationPair> s) {
        return join(p, s, true);
    }

    @Override
    public IntegPolicy meet(IntegPolicy p, Set<DelegationPair> s) {
        return meet(p, s, true);
    }

    @Override
    public IntegPolicy join(IntegPolicy p) {
        return join(p, true);
    }

    @Override
    public IntegPolicy meet(IntegPolicy p) {
        return meet(p, true);
    }

    @Override
    public IntegPolicy join(IntegPolicy p, boolean simplify) {
        return labelUtil.join(this, p, simplify);
    }

    @Override
    public IntegPolicy meet(IntegPolicy p, boolean simplify) {
        return labelUtil.meet(this, p, simplify);
    }

    @Override
    public IntegPolicy join(IntegPolicy p, Set<DelegationPair> s,
            boolean simplify) {
        return labelUtil.join(this, p, s, simplify);
    }

    @Override
    public IntegPolicy meet(IntegPolicy p, Set<DelegationPair> s,
            boolean simplify) {
        return labelUtil.meet(this, p, s, simplify);
    }
}
