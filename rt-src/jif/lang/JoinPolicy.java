package jif.lang;

import java.util.*;

/**
 * Abstract class representing the join of policies. All the policies should be
 * of the same kind, either all IntegPolicies or all ConfPolicies.
 */
public abstract class JoinPolicy extends AbstractPolicy implements Policy
{
    private Set components; // Set of Policies
    JoinPolicy(LabelUtil labelUtil, Set policies) {
        super(labelUtil);
        components = Collections.unmodifiableSet(policies);
    }
    
    public Set joinComponents() {
        return components;
    }
    
    public boolean relabelsTo(Policy pol, Set s) {
        if (this == pol || this.equals(pol)) return true;
        
        Set temp = new HashSet();
        // this == c1 join ... join cn
        // this <= pol if for all Ci, Ci <= pol
        boolean sat = true;
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Policy Ci = (Policy) i.next();
            if (!labelUtil.relabelsTo(Ci, pol, temp)) {
                sat = false;
                break;
            }
        }
        if (sat) {
            s.addAll(temp);
            return true;
        }
        
        temp.clear();
        
        // failed so far, try taking advantage of structure on the RHS
        if (pol instanceof MeetPolicy) {
            // this <= d1 meet ... meet dn if for all di
            // we have this <= di
            MeetPolicy mp = (MeetPolicy)pol;
            sat = true;
            for (Iterator i = mp.meetComponents().iterator(); i.hasNext(); ) {
                Policy Di = (Policy) i.next();
                if (!labelUtil.relabelsTo(this, Di, temp)) {
                    sat = false;
                    break;
                }
            }
            if (sat) {
                s.addAll(temp);
                return true;            
            }
        }
        if (pol instanceof JoinPolicy) {
            // this <= d1 join ... join dn if there is some di
            // such that this <= di
            JoinPolicy jp = (JoinPolicy)pol;
            for (Iterator i = jp.joinComponents().iterator(); i.hasNext(); ) {
                temp.clear();
                Policy Di = (Policy) i.next();
                if (labelUtil.relabelsTo(this, Di, temp)) {
                    s.addAll(temp);
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean equals(Object o) {
        if (o instanceof JoinPolicy) {
            JoinPolicy that = (JoinPolicy)o;            
            return this == that || this.joinComponents().equals(that.joinComponents());
        }        
        return false;
    }

    public final int hashCode() {
        return components.hashCode();
    }
    
    public final String toString() {
        String str = "";
        for (Iterator iter = components.iterator(); iter.hasNext(); ) {
            str += ((Policy)iter.next()).toString();
            if (iter.hasNext()) str += "; ";
        }
        return str;
    }
}
