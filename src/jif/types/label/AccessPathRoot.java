package jif.types.label;

import polyglot.util.Position;
import jif.types.JifContext;
import jif.types.PathMap;


/**
 * Represents a final access path root.
 * 
 * @see jif.types.label.AccessPath
 */
public abstract class AccessPathRoot extends AccessPath {
    protected AccessPathRoot(Position pos) {
        super(pos);    
    }
    
    public boolean isUninterpreted() {
        return false;
    }

    public PathMap labelcheck(JifContext A) {
        throw new UnsupportedOperationException("Cannot labelcheck an " + this.getClass());
    }
}
