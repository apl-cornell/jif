package jif.lang;

import java.util.Set;

import jif.lang.PrincipalUtil.DelegationPair;

/**
 * Represents the join of confidentiality policies
 * 
 */
public final class JoinConfPolicy extends JoinPolicy implements ConfPolicy
{
    JoinConfPolicy(LabelUtil labelUtil, Set<Policy> policies) {
        super(labelUtil, policies);
    }

    @Override
    public ConfPolicy join(ConfPolicy p, Set<DelegationPair> s) {
        return labelUtil.join(this, p, s);
    }

    @Override
    public ConfPolicy meet(ConfPolicy p, Set<DelegationPair> s) {
        return labelUtil.meet(this, p, s);
    }
    @Override
    public ConfPolicy join(ConfPolicy p) {
        return labelUtil.join(this, p);
    }

    @Override
    public ConfPolicy meet(ConfPolicy p) {
        return labelUtil.meetPol(this, p);
    }
}
