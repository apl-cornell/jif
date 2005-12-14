package jif.lang;

import java.util.*;

/**
 * A collection of IntegPolicys (implicitly meeted together)
 *  
 */
public final class IntegCollection
{
    private Set components; // Set of IntegPolicy
    IntegCollection() {
        components = Collections.EMPTY_SET;
    }

    IntegCollection(IntegPolicy integPol) {
        components = Collections.singleton(integPol);
    }
    
    IntegCollection(Set integPolicies) {
        components = Collections.unmodifiableSet(integPolicies);
    }
    
    public Set meetComponents() {
        return components;
    }

    public IntegCollection meet(IntegPolicy p) {
        return LabelUtil.meet(this, p);        
    }
    
    public boolean relabelsTo(IntegCollection l) {
        // this == {... Ci ...}
        // l == { ... Dj ... }
        // this <= l if for each Di, there exists a Ci such that Ci <= Dj
        // Check if this <= l
        
        // If this = { .. Ci .. }, check that for all i, Ci <= l
        for (Iterator i = l.meetComponents().iterator(); i.hasNext(); ) {
            IntegPolicy Ci = (IntegPolicy) i.next();
            boolean sat = false;
            
            for (Iterator j = components.iterator(); j.hasNext(); ) {
                IntegPolicy Dj = (IntegPolicy) j.next();
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
            return this.meetComponents().equals(that.joinComponents());
        }        
        return false;
    }
    
    public String componentString() {
        String str = "";
        for (Iterator iter = components.iterator(); iter.hasNext(); ) {
            str += ((IntegPolicy)iter.next()).componentString();
            if (iter.hasNext()) str += "; ";
        }
        return str;
    }
}
