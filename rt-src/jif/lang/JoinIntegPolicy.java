package jif.lang;

import java.util.*;

/**
 * Represents the join of integrity policies
 *  
 */
public final class JoinIntegPolicy extends JoinPolicy implements IntegPolicy
{
    JoinIntegPolicy(LabelUtil labelUtil, Set policies) {
        super(labelUtil, policies);
    }

    public IntegPolicy join(IntegPolicy p, Set s) {
        return labelUtil.join(this, p, s);
    }
    public IntegPolicy join(IntegPolicy p) {
        return labelUtil.join(this, p);
    }

    public IntegPolicy meet(IntegPolicy p, Set s) {
        return labelUtil.meet(this, p, s);
    }
    public IntegPolicy meet(IntegPolicy p) {
        return labelUtil.meetPol(this, p);
    }
}
