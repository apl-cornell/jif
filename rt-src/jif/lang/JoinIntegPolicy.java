package jif.lang;

import java.util.*;

/**
 * Represents the join of integrity policies
 *  
 */
public final class JoinIntegPolicy extends JoinPolicy implements IntegPolicy
{
    JoinIntegPolicy(Set policies) {
        super(policies);
    }

    public IntegPolicy join(IntegPolicy p) {
        return LabelUtil.join(this, p);
    }

    public IntegPolicy meet(IntegPolicy p) {
        return LabelUtil.meet(this, p);
    }
}
