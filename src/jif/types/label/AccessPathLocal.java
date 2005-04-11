package jif.types.label;

import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import polyglot.ast.Local;
import polyglot.types.*;
import polyglot.types.LocalInstance;
import polyglot.types.Resolver;

/**
 * TODO Documentation
 * Represent a final access path rooted at a local variable.
 */
public class AccessPathLocal extends AccessPathRoot {
    private LocalInstance li;
    public AccessPathLocal(LocalInstance li) {
        this.li = li;
    }
    
    public boolean isCanonical() { return li.isCanonical(); }
    public AccessPath subst(AccessPathRoot r, AccessPath e) {
        if (r instanceof AccessPathLocal) {            
            if (li.equals(((AccessPathLocal)r).li)) {
                return e;
            }
        }
        return this;
    }
    
    public String toString() {
        return li.name();
    }
    
    public boolean equals(Object o) {
        if (o instanceof AccessPathLocal) {
            return li.equals(((AccessPathLocal)o).li);
        }
        return false;        
    }

    public int hashCode() {
        return li.hashCode();
    }
    public Type type() {
        return li.type();
    }

    public PathMap labelcheck(JifContext A) {
    	JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
    	Label L = ts.labelOfLocal(li, A.pc());

    	PathMap X = ts.pathMap();
    	X = X.N(A.pc());
    	X = X.NV(L.join(A.pc()));
        
        return X;
    }
}
