package jif.lang;

import java.util.*;

/**
 * Represents the join of confidentiality policies
 *  
 */
public final class JoinConfPolicy extends JoinPolicy implements ConfPolicy
{
    JoinConfPolicy(LabelUtil labelUtil, Set policies) {
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
