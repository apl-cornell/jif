package jif.lang;

import java.util.*;
import java.lang.reflect.*;

public abstract class AbstractPrincipal implements Principal
{
    protected Set superiors;
    protected Map cache;

    protected AbstractPrincipal() {
	this.superiors = new HashSet();
	this.cache = new HashMap();
    }

    protected AbstractPrincipal(Set superiors) {
	this.superiors = new HashSet(superiors);
	this.cache = new HashMap();
    }

    protected abstract boolean actsForImpl(Principal principal);
    public final boolean actsFor(Principal principal)  {
        Boolean b = (Boolean)cache.get(principal);
        if (b == null) {
            b = Boolean.valueOf(actsForImpl(principal));
            cache.put(principal, b);
        }
        return b.booleanValue();
    }

    public final boolean equivalentTo(Principal principal)  {
        return this.actsFor(principal) && principal.actsFor(this);
    }

    protected Set superiorsInternal() {
        return superiors;
    }
    public final Set superiors() {
	return Collections.unmodifiableSet(superiorsInternal());
    }

    public String fullName() {
	return name();
    }

    public String toString() {
	return name();
    }
}
