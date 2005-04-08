package jif.types.label;

import polyglot.types.*;
import polyglot.types.FieldInstance;
import polyglot.types.Resolver;

/**
 * TODO Documentation
 * Represent a final access path 
 */
public class AccessPathField extends AccessPath {
    private FieldInstance fi;
    private AccessPath path;

    public AccessPathField(AccessPath path, FieldInstance fi) {
        this.fi = fi;
        this.path = path;
    }
    
    public boolean isCanonical() { return path.isCanonical(); }
    public String translate(Resolver c) { return null;
    }
    public AccessPath subst(AccessPathRoot r, AccessPath e) {
        AccessPath newPath = path.subst(r, e);
        if (newPath == path) return this;
        
        return new AccessPathField(newPath, fi);
    }
    public String toString() {
        return path + "." + fi.name();
    }
    public boolean equals(Object o) {
        if (o instanceof AccessPathField) {
            AccessPathField that = (AccessPathField)o; 
            return this.fi.name().equals(that.fi.name()) && this.path.equals(that.path);
        }
        return false;        
    }

    public Type type() {
        return fi.type();
    }
}
