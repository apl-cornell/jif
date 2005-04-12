package jif.types.label;

import java.io.Serializable;

import jif.types.JifContext;
import jif.types.PathMap;
import polyglot.types.*;

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

    /**
     * Go through the path, check that all the type information is 
     * set correctly, and check that every field access is final. 
     * @param A
     */
    public void verify(JifContext A) throws SemanticException {}
    public final String translate(Resolver c) { throw new UnsupportedOperationException(); }
}
