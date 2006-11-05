package jif.lang;

import java.util.*;

/**
 * Represents the meet of integrity policies
 *  
 */
public final class MeetIntegPolicy extends MeetPolicy implements IntegPolicy
{
    MeetIntegPolicy(Set policies) {
        super(policies);
    }

    public IntegPolicy join(IntegPolicy p, Set s) {
        return LabelUtil.join(this, p, s);
    }

    public IntegPolicy meet(IntegPolicy p, Set s) {
        return LabelUtil.meet(this, p, s);
    }
    public IntegPolicy join(IntegPolicy p) {
        return LabelUtil.join(this, p);
    }

    public IntegPolicy meet(IntegPolicy p) {
        return LabelUtil.meetPol(this, p);
    }
}
