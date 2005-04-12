package jif.types.label;

import jif.types.JifTypeSystem;
import jif.types.principal.Principal;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/**
 * TODO Documentation
 * Represent a final access path consisting of a constant label or principal,
 * that is, either a NewLabel expression or an External principal.
 */
public class AccessPathConstant extends AccessPathRoot {
    /**
     * Either a label or a principal
     */
    private Object constantValue;
    private boolean isLabel;
    public AccessPathConstant(Label label, Position pos) {
        super(pos);
        this.constantValue = label;
        this.isLabel = true;
    }
    public AccessPathConstant(Principal principal, Position pos) {
        super(pos);
        this.constantValue = principal;
        this.isLabel = false;
    }
    
    public Object constantValue() {
        return constantValue;
    }
    public boolean isLabelConstant() {
        return this.isLabel;
    }
    public boolean isPrincipalConstant() {
        return !this.isLabel;
    }
    
    public boolean isCanonical() { return true; }
    public AccessPath subst(AccessPathRoot r, AccessPath e) {
        throw new InternalCompilerError("Shouldn't be calling subst on an AccessPathConstant!");
    }
    
    public String toString() {
        return constantValue.toString();
    }
    
    public boolean equals(Object o) {
        if (o instanceof AccessPathConstant) {
            AccessPathConstant that = (AccessPathConstant)o;
            return constantValue.equals(that.constantValue);
        }
        return false;        
    }
    public int hashCode() {
        return constantValue.hashCode();
    }
    public Type type() {
        if (isLabel) {
            Label l = (Label)constantValue;
            return ((JifTypeSystem)l.typeSystem()).Label();
        }
        Principal p = (Principal)constantValue;
        return ((JifTypeSystem)p.typeSystem()).Principal();
    }
}