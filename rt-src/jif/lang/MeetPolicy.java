package jif.lang;

import java.util.*;

/**
 * Abstract class representing the meet of policies. All the policies should be
 * of the same kind, either all IntegPolicies or all ConfPolicies.
 */
abstract class MeetPolicy extends AbstractPolicy implements Policy
{
    private Set components; // Set of Policies    
    MeetPolicy(Set policies) {
        components = Collections.unmodifiableSet(policies);
    }
    
    public Set meetComponents() {
        return components;
    }
    
    public boolean relabelsTo(Policy pol) {
        // this == c1 meet ... meet cn
        // this <= pol if there is a Ci such that Ci <= pol
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Policy Ci = (Policy) i.next();
            if (Ci.relabelsTo(pol)) {
                return true;
            }
        }
        
        // failed so far, try taking advantage of structure on the RHS
        if (pol instanceof MeetPolicy) {
            // this <= d1 meet ... meet dn if for all di
            // we have this <= di
            MeetPolicy mp = (MeetPolicy)pol;
            boolean sat = true;
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
        if (o instanceof MeetPolicy) {
            MeetPolicy that = (MeetPolicy)o;
            return this.meetComponents().equals(that.meetComponents());
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
            if (iter.hasNext()) str += " meet ";
        }
        return str;
    }
}
