package jif.lang;

import java.util.*;

/**
 * Represents the meet of confidentiality policies
 *  
 */
public final class MeetConfPolicy extends MeetPolicy implements ConfPolicy
{
    MeetConfPolicy(LabelUtil labelUtil, Set policies) {
        super(labelUtil, policies);
    }

    public ConfPolicy join(ConfPolicy p, Set s) {
        return labelUtil.join(this, p, s);
    }
    public ConfPolicy meet(ConfPolicy p, Set s) {
        return labelUtil.meet(this, p, s);
    }
    public ConfPolicy join(ConfPolicy p) {
        return labelUtil.join(this, p);
    }
    public ConfPolicy meet(ConfPolicy p) {
        return labelUtil.meetPol(this, p);
    }
}
