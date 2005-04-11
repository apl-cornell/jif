package jif.types.label;

import java.io.Serializable;

import jif.types.JifContext;
import jif.types.PathMap;

import polyglot.types.Resolver;
import polyglot.types.Type;

/**
 * TODO Documentation
 * Represent a final access path 
 */
public abstract class AccessPath implements Serializable {
    public abstract boolean isCanonical();
    public abstract AccessPath subst(AccessPathRoot r, AccessPath e);
    public abstract Type type();
    public abstract int hashCode();
    public abstract PathMap labelcheck(JifContext A);

    public final String translate(Resolver c) { throw new UnsupportedOperationException(); }
}
