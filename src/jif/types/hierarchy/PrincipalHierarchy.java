package jif.types.hierarchy;

import java.util.*;

import jif.types.principal.*;

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
    
    private static void addAlreadyReported(Map alreadyReported, Principal p, Principal q) {
        // record the fact that we have already reported that q actsfor p
        Set s = (Set)alreadyReported.get(q);
        if (s == null) {
            s = new HashSet();
            alreadyReported.put(q, s);
        }
        s.add(p);
    }
    private static boolean isAlreadyReported(Map alreadyReported, Principal p, Principal q) {
        Set s = (Set)alreadyReported.get(p);        
        if (s != null) {
            return s.contains(q);
        }
        return false;
    }
    public String actsForString() {
        StringBuffer sb = new StringBuffer();
        Map alreadyReported = new HashMap();
        boolean needsComma = false;
        for (Iterator i = actsfor.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry) i.next();
            Principal p = (Principal) e.getKey();
            Set a = (Set) e.getValue();

            for (Iterator j = a.iterator(); j.hasNext(); ) {
                Principal q = (Principal) j.next();
                if (isAlreadyReported(alreadyReported, p, q)) {
                    continue;
                }
                if (needsComma) {
                    sb.append(", ");
                }                
                sb.append("(");
                sb.append(p.toString());
                if (actsFor(q, p)) {
                    // q also acts for p
                    sb.append(" equiv ");
                    addAlreadyReported(alreadyReported, p, q);
                }
                else {
                    sb.append(" actsFor ");
                }
                sb.append(q.toString());
                sb.append(")");
                needsComma = true;
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
	    s = new LinkedHashSet();
	    actsfor.put(actor, s);
	}

	s.add(granter);
    }

    public boolean actsFor(Principal actor, Principal granter) {
	return actsFor(actor, granter, new LinkedHashSet());
    }

    protected boolean actsFor(Principal actor, Principal granter, Set visited) {
        if (actor.isTopPrincipal()) return true;
        if (granter.isBottomPrincipal()) return true;
        
	if (visited.contains(actor)) { 
	    return false;
        }
	
	// Check the reflexive part of actsFor relation.
	if (actor.equals(granter)) {
	    return true;
	}

	Set s = (Set) actsfor.get(actor);

	if (s != null && s.contains(granter)) {
	    return true;
	}

        // special cases for conjunctive and disjunctive principals.
        if (actor instanceof ConjunctivePrincipal) {
            ConjunctivePrincipal cp = (ConjunctivePrincipal)actor;
            if (actsFor(cp.conjunctLeft(), granter, visited) || 
                    actsFor(cp.conjunctRight(), granter, visited)) {
                // Cache the result.
                s.add(granter);
                return true;                
            }            
        }
        if (actor instanceof DisjunctivePrincipal) {
            DisjunctivePrincipal dp = (DisjunctivePrincipal)actor;
            if (actsFor(dp.disjunctLeft(), granter, visited) && 
                    actsFor(dp.disjunctRight(), granter, visited)) {
                return true;                
            }            
        }
        
        if (granter instanceof DisjunctivePrincipal) {
            DisjunctivePrincipal dp = (DisjunctivePrincipal)granter;
            if (actsFor(actor, dp.disjunctLeft(), visited) || 
                    actsFor(actor, dp.disjunctRight(), visited)) {
                return true;                
            }            
        }

        if (granter instanceof ConjunctivePrincipal) {
            ConjunctivePrincipal cp = (ConjunctivePrincipal)granter;
            if (actsFor(actor, cp.conjunctLeft(), visited) && 
                    actsFor(actor, cp.conjunctRight(), visited)) {
                return true;                
            }            
        }

        // Check the transitive part of actsFor relation.
        if (s == null) return false;
        visited.add(actor);

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
	    dup.actsfor.put(p, new LinkedHashSet(s));
	}

	return dup;
    }

    public void clear() {
	actsfor.clear();
    }
}
