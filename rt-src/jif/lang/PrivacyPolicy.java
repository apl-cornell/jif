package jif.lang;

import java.util.*;

public class PrivacyPolicy implements Policy
{
    Principal owner;
    Set readers;

    public PrivacyPolicy(Principal owner, Collection readers) {
	this.owner = owner;
	this.readers = new HashSet(readers);
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

    public boolean relabelsTo(Policy p) {
	if (!(p instanceof PrivacyPolicy))
	    return false;

	PrivacyPolicy pp = (PrivacyPolicy) p;
	
	// this = { o  : .. ri  .. }
	// p    = { o' : .. rj' .. }

	// o' >= o?
	
	if (! pp.owner.actsFor(owner)) {
	    return false;
	}

	// for all j . rj' >= o || exists i . rj' >= ri
	for (Iterator j = pp.readers.iterator(); j.hasNext(); ) {
	    Principal rj = (Principal) j.next();

	    boolean sat = false;

	    if (rj.actsFor(owner)) {
		sat = true;
	    }
	    else {
		for (Iterator i = readers.iterator(); i.hasNext(); ) {
		    Principal ri = (Principal) i.next();

		    if (rj.actsFor(ri)) {
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
	return owner.hashCode() + readers.size();
    }

    public boolean equals(Object o) {
	if (! (o instanceof PrivacyPolicy)) {
	    return false;
	}

	PrivacyPolicy policy = (PrivacyPolicy) o;

	if (! owner.equals(policy.owner)) {
	    return false;
	}

	if (readers.size() != policy.readers.size()) {
	    return false;
	}

	if (! readers.containsAll(policy.readers)) {
	    return false;
	}

	return true;
    }
    
    public String toString() {
	String str = owner.name() + ": ";
	for (Iterator iter = readers.iterator(); iter.hasNext(); ) {
	    Principal reader = (Principal) iter.next();
	    str += reader.name();
	    if (iter.hasNext()) str += ",";
	}
	return str;
    }
}
