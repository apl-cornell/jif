package jif.lang;

import java.util.*;

/**
 * A collection of ConfPolicys (implicitly joined together)
 *  
 */
public final class ConfCollection
{
    private Set components; // Set of ConfPolicy
    ConfCollection() {
        components = Collections.EMPTY_SET;
    }
    ConfCollection(ConfPolicy confPol) {
        components = Collections.singleton(confPol);
    }
    
    ConfCollection(Set confPolicies) {
        components = Collections.unmodifiableSet(confPolicies);
    }
    
    public Set joinComponents() {
        return components;
    }
    
    public ConfCollection join(ConfPolicy p) {
        return LabelUtil.join(this, p);        
    }
    public boolean relabelsTo(ConfCollection l) {
        // this == {... Ci ...}
        // l == { ... Dj ... }
        // this <= l if for each Ci, there exists a Dj such that Ci <= Dj
        // Check if this <= l
        
        // If this = { .. Ci .. }, check that for all i, Ci <= l
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            ConfPolicy Ci = (ConfPolicy) i.next();
            boolean sat = false;
            
            for (Iterator j = l.joinComponents().iterator(); j.hasNext(); ) {
                ConfPolicy Dj = (ConfPolicy) j.next();
                if (Ci.relabelsTo(Dj)) {
                    sat = true;
                    break;
                }
            }
            if (!sat) return false;
        }
        return true;
    }
    
    public boolean equals(Object o) {
        if (o instanceof Label) {
            Label that = (Label)o;
            return this.joinComponents().equals(that.joinComponents());
        }        
        return false;
    }
    
    public String componentString() {
        String str = "";
        for (Iterator iter = components.iterator(); iter.hasNext(); ) {
            str += ((Label)iter.next()).componentString();
            if (iter.hasNext()) str += "; ";
        }
        return str;
    }
}
