package jif.lang;

import java.util.Set;

import jif.lang.PrincipalUtil.DelegationPair;

/**
 * Represents the meet of integrity policies
 * 
 */
public final class MeetIntegPolicy extends MeetPolicy implements IntegPolicy
{
    MeetIntegPolicy(LabelUtil labelUtil, Set<Policy> policies) {
        super(labelUtil, policies);
    }

    @Override
    public IntegPolicy join(IntegPolicy p, Set<DelegationPair> s) {
        return labelUtil.join(this, p, s);
    }

    @Override
    public IntegPolicy meet(IntegPolicy p, Set<DelegationPair> s) {
        return labelUtil.meet(this, p, s);
    }
    @Override
    public IntegPolicy join(IntegPolicy p) {
        return labelUtil.join(this, p);
    }

    @Override
    public IntegPolicy meet(IntegPolicy p) {
        return labelUtil.meetPol(this, p);
    }
}
