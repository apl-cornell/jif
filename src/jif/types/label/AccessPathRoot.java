package jif.types.label;

import polyglot.types.LocalInstance;
import polyglot.types.Resolver;

/**
 * TODO Documentation
 * Represent a final access path 
 */
public class AccessPathRoot extends AccessPath {
    private LocalInstance li;
    public AccessPathRoot(LocalInstance li) {
        this.li = li;
    }
    
    public boolean isCanonical() { return li.isCanonical(); }
    public String translate(Resolver c) { return null;
    }
    public AccessPath subst(AccessPathRoot r, AccessPath e) {
        if (li.equals(r.li)) {
            return e;
        }
        return this;
    }
    
    public boolean equals(Object o) {
        if (o instanceof AccessPathRoot) {
            return li.equals(((AccessPathRoot)o).li);
        }
        return false;        
    }
}
