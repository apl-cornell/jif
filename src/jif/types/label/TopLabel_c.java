package jif.types.label;


import jif.types.JifTypeSystem;
import jif.types.hierarchy.LabelEnv;
import polyglot.types.*;
import polyglot.types.Resolver;
import polyglot.types.TypeObject;
import polyglot.util.*;

/** An implementation of the <code>TopLabel</code> interface. 
 */
public class TopLabel_c extends Label_c implements TopLabel {
    protected TopLabel_c() {
    }
    
    public TopLabel_c(JifTypeSystem ts, Position pos) {
        super(ts, pos);
        setDescription("Top of the label lattice, the most private label possible");
    }
    
    public boolean isTop() { return true; }    
    public boolean isComparable() { return true; }    
    public boolean isEnumerable() { return false; }    
    public boolean isCanonical() { return true; }    
    public boolean isRuntimeRepresentable() { return false; }
    public boolean isCovariant() { return false; }
    
    public String componentString() {
        return "<top>";
    }    
    public String toString() {
        return "<top>";
    }    
    public boolean equalsImpl(TypeObject o) {
        return o instanceof TopLabel;
    }    
    public int hashCode() { return 390230; }
    public boolean leq_(Label L, LabelEnv env) {
        return L.isTop();
    }
    public void translate(Resolver c, CodeWriter w) {
        throw new InternalCompilerError("Cannot translate label \"" + this + "\".");
    }
    public Label subst(LocalInstance arg, Label l) {
        return this;
    }
    public Label subst(AccessPathRoot r, AccessPath e) {
        return this;
    }
}
