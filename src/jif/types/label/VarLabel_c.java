package jif.types.label;

import java.util.Collections;
import java.util.Set;

import jif.types.JifTypeSystem;
import jif.types.hierarchy.LabelEnv;
import polyglot.types.Resolver;
import polyglot.types.TypeObject;
import polyglot.util.*;

/** An implementation of the <code>VarLabel</code> interface. 
 */
public class VarLabel_c extends Label_c implements VarLabel {
    private final transient int uid = ++counter;
    private static int counter = 0;
    
    protected VarLabel_c() {
    }
    
    public VarLabel_c(JifTypeSystem ts, Position pos) {
        super(ts, pos);
    }
    
    public boolean isEnumerable() { return true; }
    public boolean isComparable() { return false; }    
    public boolean isCanonical() { return true; }     
    public boolean isRuntimeRepresentable() { return false; }
    public boolean isCovariant() { return false; }
    
    public Set variables() { return Collections.singleton(this); }
    
    public String componentString() {
        return "<var " + uid + ">";
    }    
    public boolean equalsImpl(TypeObject o) {
        return this == o;
    }    
    public int hashCode() { return -56393 + uid; }
    
    public boolean leq_(Label L, LabelEnv env) {   
        throw new InternalCompilerError("Cannot compare " + this + ".");
    }
    
    public void translate(Resolver c, CodeWriter w) {
        throw new InternalCompilerError("Cannot translate \"" + this + "\".");
    }
}
