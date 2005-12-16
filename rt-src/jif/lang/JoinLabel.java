package jif.lang;

import java.util.*;

/**
 * A Label is the runtime representation of a Jif label. A Label consists of a
 * set of components, each of which is a {@link jif.lang.IntegPolicy Policy}.
 *  
 */
public final class JoinLabel implements Label
{
    private Set components; // Set of Policies
    JoinLabel() {
        components = Collections.EMPTY_SET;
    }
    
    JoinLabel(Set policies) {
        components = Collections.unmodifiableSet(policies);
    }
    
    public Set joinComponents() {
        return components;
    }
    
    public boolean relabelsTo(Label l) {
        // this == {... Ci ...}
        // l == { ... Dj ... }
        // this <= l if for each Ci, there exists a Dj such that Ci <= Dj
        // Check if this <= l
        
        // If this = { .. Ci .. }, check that for all i, Ci <= l
        for (Iterator i = components.iterator(); i.hasNext(); ) {
            Policy Ci = (Policy) i.next();
            boolean sat = false;
            
            for (Iterator j = l.joinComponents().iterator(); j.hasNext(); ) {
                Policy Dj = (Policy) j.next();
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
    public final String toString() {
        return "{" + this.componentString() + "}";
    }
    
    public final Label join(Label l) {
        return LabelUtil.join(this, l);
    }
    public String componentString() {
        String str = "";
        for (Iterator iter = components.iterator(); iter.hasNext(); ) {
            str += ((Policy)iter.next()).componentString();
            if (iter.hasNext()) str += "; ";
        }
        return str;
    }
}
