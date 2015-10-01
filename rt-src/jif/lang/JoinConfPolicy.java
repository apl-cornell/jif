package jif.lang;

import java.util.Set;

import jif.lang.PrincipalUtil.DelegationPair;

/**
 * Represents the join of confidentiality policies
 * 
 */
public final class JoinConfPolicy extends JoinPolicy implements ConfPolicy {
    JoinConfPolicy(LabelUtil labelUtil, Set<Policy> policies) {
        super(labelUtil, policies);
    }

    @Override
    public ConfPolicy join(ConfPolicy p, Set<DelegationPair> s) {
        return join(p, s, true);
    }

    @Override
    public ConfPolicy meet(ConfPolicy p, Set<DelegationPair> s) {
        return meet(p, s, true);
    }

    @Override
    public ConfPolicy join(ConfPolicy p) {
        return join(p, true);
    }

    @Override
    public ConfPolicy meet(ConfPolicy p) {
        return meet(p, true);
    }

    @Override
    public ConfPolicy join(ConfPolicy p, boolean simplify) {
        return labelUtil.join(this, p, simplify);
    }

    @Override
    public ConfPolicy meet(ConfPolicy p, boolean simplify) {
        return labelUtil.meet(this, p, simplify);
    }

    @Override
    public ConfPolicy join(ConfPolicy p, Set<DelegationPair> s,
            boolean simplify) {
        return labelUtil.join(this, p, s, simplify);
    }

    @Override
    public ConfPolicy meet(ConfPolicy p, Set<DelegationPair> s,
            boolean simplify) {
        return labelUtil.meet(this, p, s, simplify);
    }
}
