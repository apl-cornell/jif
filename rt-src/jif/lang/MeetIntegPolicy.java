package jif.lang;

import java.util.*;

/**
 * Represents the meet of integrity policies
 *  
 */
public final class MeetIntegPolicy extends MeetPolicy implements IntegPolicy
{
    MeetIntegPolicy(LabelUtil labelUtil, Set policies) {
        super(labelUtil, policies);
    }

    public IntegPolicy join(IntegPolicy p, Set s) {
        return labelUtil.joinImpl(this, p, s);
    }

    public IntegPolicy meet(IntegPolicy p, Set s) {
        return labelUtil.meetImpl(this, p, s);
    }
    public IntegPolicy join(IntegPolicy p) {
        return labelUtil.joinImpl(this, p);
    }

    public IntegPolicy meet(IntegPolicy p) {
        return labelUtil.meetPolImpl(this, p);
    }
}
