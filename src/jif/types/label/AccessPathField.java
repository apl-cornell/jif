package jif.types.label;

import polyglot.types.Resolver;

/**
 * TODO Documentation
 * Represent a final access path 
 */
public class AccessPathField extends AccessPath {
    private String fieldName;
    private AccessPath path;

    public AccessPathField(AccessPath path, String fieldName) {
        this.fieldName = fieldName;
        this.path = path;
    }
    
    public boolean isCanonical() { return path.isCanonical(); }
    public String translate(Resolver c) { return null;
    }
    public AccessPath subst(AccessPathRoot r, AccessPath e) {
        AccessPath newPath = path.subst(r, e);
        if (newPath == path) return this;
        
        return new AccessPathField(newPath, fieldName);
    }
    public boolean equals(Object o) {
        if (o instanceof AccessPathField) {
            AccessPathField that = (AccessPathField)o; 
            return this.fieldName.equals(that.fieldName) && this.path.equals(that.path);
        }
        return false;        
    }
}
