package jif.lang;

import java.util.*;
import java.lang.reflect.*;

class DynamicPrincipal extends AbstractPrincipal
{
    static Map cache = new HashMap();

    private final String name;

    DynamicPrincipal(String name) {
	this.name = name;
    }

    static DynamicPrincipal create(String name) {
        DynamicPrincipal p = (DynamicPrincipal) cache.get(name);

	if (p == null) {
	    p = new DynamicPrincipal(name);
	    cache.put(name, p);
	}

	return p;
    }

    protected boolean actsForImpl(Principal principal) {
        return principal == this;
    }

    public String name() {
        return name;
    }

    public String fullName() {
        return name();
    }
}
