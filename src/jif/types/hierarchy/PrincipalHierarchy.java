package jif.types.hierarchy;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jif.types.principal.Principal;

/** The principal hierarchy that defines the acts-for relationships
 *  between principals. 
 */
public class PrincipalHierarchy {
    /**
     * Map from Principal to Set[Principal], where if p actsfor p', then
     * p' is in the set actsfor.get(p)
     */
    private final Map actsfor;

    public PrincipalHierarchy() {
	this.actsfor = new HashMap();
    }

    public String toString() {
	return "[" + actsForString()+ "]";
    }
    
    public String actsForString() {
        StringBuffer sb = new StringBuffer();
        for (Iterator i = actsfor.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry) i.next();
            Principal p = (Principal) e.getKey();
            Set a = (Set) e.getValue();

            for (Iterator j = a.iterator(); j.hasNext(); ) {
                Principal q = (Principal) j.next();
                sb.append("(");
                sb.append(p.toString());
                sb.append(" actsFor ");
                sb.append(q.toString());
                sb.append(")");
                if (j.hasNext()) {
                    sb.append(", ");
                }                
            }
            if (i.hasNext() && !a.isEmpty()) {
                sb.append(", ");
            }                
        }
        return sb.toString();
    }

    public boolean isEmpty() {
        return actsfor.isEmpty();
    }
    public void add(Principal actor, Principal granter) {
	Set s = (Set) actsfor.get(actor);

	if (s == null) {
	    // create a new set of granting principals
	    s = new HashSet();
	    actsfor.put(actor, s);
	}

	s.add(granter);
    }

    public boolean actsFor(Principal actor, Principal granter) {
	return actsFor(actor, granter, new HashSet());
    }

    protected boolean actsFor(Principal actor, Principal granter, Set visited) {
	if (visited.contains(actor)) 
	    return false;
	
	// Check the reflexive part of actsFor relation.
	if (actor.equals(granter)) {
	    return true;
	}

	Set s = (Set) actsfor.get(actor);

	if (s == null) {
	    return false;
	}

	if (s.contains(granter)) {
	    return true;
	}

	visited.add(actor);

	// Check the transitive part of actsFor relation.
	for (Iterator iter = s.iterator(); iter.hasNext(); ) {
	    Principal p = (Principal) iter.next();

	    if (actsFor(p, granter, visited)) {
		// Cache the result.
		s.add(granter);
		return true;
	    }
	}

	return false;
    }

    public boolean actsFor(Collection actorGrp, Collection grantorGrp) {
	for (Iterator i = grantorGrp.iterator(); i.hasNext(); ) {
	    Principal gi = (Principal) i.next();
	    boolean sat = false;
	    for (Iterator j = actorGrp.iterator(); j.hasNext(); ) {
		Principal aj = (Principal) j.next();

		if (actsFor(aj, gi)) {
		    sat = true;
		    break;
		}
	    }

	    if (! sat) 
		return false;
	}
	return true;
    }

    public PrincipalHierarchy copy() {
        PrincipalHierarchy dup = new PrincipalHierarchy();

	for (Iterator i = actsfor.entrySet().iterator(); i.hasNext(); ) {
	    Map.Entry e = (Map.Entry) i.next();
	    Principal p = (Principal) e.getKey();
	    Set s = (Set) e.getValue();
	    dup.actsfor.put(p, new HashSet(s));
	}

	return dup;
    }

    public void clear() {
	actsfor.clear();
    }
}
