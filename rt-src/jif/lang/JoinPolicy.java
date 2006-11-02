package jif.lang;

import java.util.*;

/**
 * Abstract class representing the join of policies. All the policies should be
 * of the same kind, either all IntegPolicies or all ConfPolicies.
 */
abstract class JoinPolicy extends AbstractPolicy implements Policy
{
    private Set components; // Set of Policies
    JoinPolicy(Set policies) {
        components = Collections.unmodifiableSet(policies);
    }
    
    public Set joinComponents() {
        return components;
    }
    
    public boolean relabelsTo(Policy pol) {
        if (this == pol || this.equals(pol)) return true;
        
        // this == c1 join ... join cn
        // this <= pol if for all Ci, Ci <= pol
        boolean sat = true;
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Policy Ci = (Policy) i.next();
            if (!Ci.relabelsTo(pol)) {
                sat = false;
                break;
            }
        }
        if (sat) return true;
        
        // failed so far, try taking advantage of structure on the RHS
        if (pol instanceof MeetPolicy) {
            // this <= d1 meet ... meet dn if for all di
            // we have this <= di
            MeetPolicy mp = (MeetPolicy)pol;
            sat = true;
            for (Iterator i = mp.meetComponents().iterator(); i.hasNext(); ) {
                Policy Di = (Policy) i.next();
                if (!this.relabelsTo(Di)) {
                    sat = false;
                    break;
                }
            }
            if (sat) return true;            
        }
        if (pol instanceof JoinPolicy) {
            // this <= d1 join ... join dn if there is some di
            // such that this <= di
            JoinPolicy jp = (JoinPolicy)pol;
            for (Iterator i = jp.joinComponents().iterator(); i.hasNext(); ) {
                Policy Di = (Policy) i.next();
                if (this.relabelsTo(Di)) {
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
