package jif.types.label;

import polyglot.types.Resolver;

/**
 * TODO Documentation
 * Represent a final access path 
 */
public abstract class AccessPath {
    public abstract boolean isCanonical();
    public abstract String translate(Resolver c);
    public abstract AccessPath subst(AccessPathRoot r, AccessPath e);
}
