package jif.lang;

import java.util.*;

/**
 * Represents the meet of confidentiality policies
 *  
 */
public final class MeetConfPolicy extends MeetPolicy implements ConfPolicy
{
    MeetConfPolicy(Set policies) {
        super(policies);
    }

    public ConfPolicy join(ConfPolicy p, Set s) {
        return LabelUtil.join(this, p, s);
    }
    public ConfPolicy meet(ConfPolicy p, Set s) {
        return LabelUtil.meet(this, p, s);
    }
    public ConfPolicy join(ConfPolicy p) {
        return LabelUtil.join(this, p);
    }
    public ConfPolicy meet(ConfPolicy p) {
        return LabelUtil.meetPol(this, p);
    }
}
