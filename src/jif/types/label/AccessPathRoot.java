package jif.types.label;

import polyglot.util.Position;
import jif.types.JifContext;
import jif.types.PathMap;


/**
 * TODO Documentation
 * Represent a final access path root 
 */
public abstract class AccessPathRoot extends AccessPath {
    protected AccessPathRoot(Position pos) {
        super(pos);    
    }
    
    public PathMap labelcheck(JifContext A) {
        throw new UnsupportedOperationException("Cannot labelcheck an " + this.getClass());
    }
}
