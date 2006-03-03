package jif.types.label;

import java.util.Collection;
import java.util.Set;

import jif.types.JifTypeSystem;
import jif.types.hierarchy.LabelEnv;
import polyglot.types.*;
import polyglot.types.Resolver;
import polyglot.types.TypeObject;
import polyglot.util.*;

/** An implementation of the <code>UnknownLabel</code> interface. 
 */
public class UnknownLabel_c extends Label_c implements UnknownLabel
{
    public UnknownLabel_c(JifTypeSystem ts, Position pos) {
        super(ts, pos);
    }
    
    public Collection components() {
        throw new InternalCompilerError("Cannot list components of " + this);
    }
    
    public boolean isComparable() { return false; }
    public boolean isEnumerable() { return false; }
    public boolean isCanonical() { return false; }
    public boolean isDisambiguatedImpl() { return false; }     
    public boolean isCovariant() { return false; }
    public boolean isRuntimeRepresentable() { return false; }
    
    public String componentString(Set printedLabels) {
        return "<unknown label>";
    }    
    public String toString() {
        return "<unknown label>";
    }
    
    public boolean equalsImpl(TypeObject o) {
        return o == this;
    }    
    public int hashCode() { return 234334; }
    public boolean leq_(Label L, LabelEnv env, LabelEnv.SearchState state) {
        throw new InternalCompilerError("Cannot compare unknown label.");
    }    
}
