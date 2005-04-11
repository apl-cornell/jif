package jif.types.label;

import jif.types.JifContext;
import jif.types.PathMap;


/**
 * TODO Documentation
 * Represent a final access path root 
 */
public abstract class AccessPathRoot extends AccessPath {
    public PathMap labelcheck(JifContext A) {
        throw new UnsupportedOperationException("Cannot labelcheck an " + this.getClass());
    }
}
