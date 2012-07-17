package jif.lang;

import java.util.*;

public class PrincipalSet
{
    private Set<Principal> set;

    public PrincipalSet() {
	set = new LinkedHashSet<Principal>();
    }

    public PrincipalSet add(Principal p) {
	PrincipalSet ps = new PrincipalSet();
	ps.set.addAll(set);
	ps.set.add(p);
	return ps;
    }

    Set<Principal> getSet() {
	return set;
    }
}
