package jif.types.label;

import java.io.Serializable;

import polyglot.types.Resolver;

/**
 * TODO Documentation
 * Represent a final access path 
 */
public abstract class AccessPath implements Serializable {
    public abstract boolean isCanonical();
    public abstract String translate(Resolver c);
    public abstract AccessPath subst(AccessPathRoot r, AccessPath e);
}
