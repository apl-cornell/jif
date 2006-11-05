package jif.lang;

import java.util.*;

/**
 * Represents the join of confidentiality policies
 *  
 */
public final class JoinConfPolicy extends JoinPolicy implements ConfPolicy
{
    JoinConfPolicy(Set policies) {
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
