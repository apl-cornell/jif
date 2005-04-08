package jif.types.label;

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
    public String translate(Resolver c) { return null;
    }
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

    public Type type() {
        return li.type();
    }
}
