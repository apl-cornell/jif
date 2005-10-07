package jif.lang;

import java.util.*;

public class PrivacyPolicy extends AbstractLabel implements Policy, Label
{
    private final Principal owner;
    private final Set readers;

    public PrivacyPolicy(Principal owner, Collection readers) {
	this.owner = owner;
	if (readers == null) readers = Collections.EMPTY_SET; 
	this.readers = Collections.unmodifiableSet(new HashSet(readers));
    }

    public PrivacyPolicy(Principal owner, PrincipalSet readers) {
	this(owner, readers.getSet());
    }
    
    public Principal owner() {
	return owner;
    }
    
    public Set readers() {
	return readers;
    }

    public boolean relabelsTo(Label l) {
        if (l instanceof Policy) {
            return relabelsTo((Policy)l);
        }
        if (l instanceof ReadableByPrinLabel) {
            ReadableByPrinLabel rbp = (ReadableByPrinLabel)l;
            
            // see if there is a reader (or the owner) that
            // rbp.reader can act for
            if (PrincipalUtil.actsFor(rbp.reader(), owner)) {
                return true;
            }

            for (Iterator j = readers.iterator(); j.hasNext(); ) {
                Principal rj = (Principal) j.next();
                if (PrincipalUtil.actsFor(rbp.reader() , rj)) {
                    return true;
                }
            }
            return false;
            
        }
        if (l instanceof JoinLabel) {
            // see if there is a component that we relabel to
            JoinLabel jl = (JoinLabel)l;
            for (Iterator iter = jl.components().iterator(); iter.hasNext(); ) {
                Label comp = (Label)iter.next();
                if (this.relabelsTo(comp)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean relabelsTo(Policy p) {
	if (!(p instanceof PrivacyPolicy))
	    return false;

	PrivacyPolicy pp = (PrivacyPolicy) p;
	
	// this = { o  : .. ri  .. }
	// p    = { o' : .. rj' .. }

	// o' >= o?
	
	if (! PrincipalUtil.actsFor(pp.owner, owner)) {
	    return false;
	}

	// for all j . rj' >= o || exists i . rj' >= ri
	for (Iterator j = pp.readers.iterator(); j.hasNext(); ) {
	    Principal rj = (Principal) j.next();

	    boolean sat = false;

	    if (PrincipalUtil.actsFor(rj, owner)) {
		sat = true;
	    }
	    else {
		for (Iterator i = readers.iterator(); i.hasNext(); ) {
		    Principal ri = (Principal) i.next();

		    if (PrincipalUtil.actsFor(rj, ri)) {
			sat = true;
			break;
		    }
		}
	    }

	    if (! sat) {
		return false;
	    }
	}
	return true;
    }

    public int hashCode() {
	return (owner==null?0:owner.hashCode()) + readers.size();
    }

    public boolean equals(Object o) {
	if (! (o instanceof PrivacyPolicy)) {
	    return false;
	}

	PrivacyPolicy policy = (PrivacyPolicy) o;

	if (owner == policy || (owner != null && owner.equals(policy.owner)
            && policy.owner != null && policy.owner.equals(owner))) {
	    return readers.containsAll(policy.readers) && 
	            policy.readers.containsAll(readers);
	}

	return false;
    }
    
    public String componentString() {
	String str = (owner == null?"<null>":owner.name()) + ": ";
	for (Iterator iter = readers.iterator(); iter.hasNext(); ) {
	    Principal reader = (Principal) iter.next();
	    str += (reader == null?"<null>":reader.name());
	    if (iter.hasNext()) str += ",";
	}
	return str;
    }

}
