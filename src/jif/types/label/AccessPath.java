package jif.types.label;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import jif.types.JifContext;
import jif.types.PathMap;
import polyglot.types.*;
import polyglot.util.Position;

/**
 * TODO Documentation
 * Represent a final access path 
 */
public abstract class AccessPath implements Serializable {
    private Position position;
    protected AccessPath(Position pos) { this.position = pos; }
   
    public abstract boolean isCanonical();
    public abstract AccessPath subst(AccessPathRoot r, AccessPath e);
    public abstract Type type();
    public abstract int hashCode();
    public abstract PathMap labelcheck(JifContext A);
    public final Position position() { return position; }

    /**
     * Go through the path, check that all the type information is 
     * set correctly, and check that every field access is final. 
     * @param A
     */
    public void verify(JifContext A) throws SemanticException {}
    public String exprString() { return toString(); }

    /**
     * Return a list of types that may be thrown as a result of the
     * runtime evaluation of this path.
     */
    public List throwTypes(TypeSystem ts) {
        return Collections.EMPTY_LIST;
    }
}
