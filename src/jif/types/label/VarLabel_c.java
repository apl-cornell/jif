package jif.types.label;

import java.util.Collections;
import java.util.Set;

import jif.types.JifTypeSystem;
import jif.types.hierarchy.LabelEnv;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.types.Resolver;
import polyglot.types.TypeObject;
import polyglot.util.*;

/** An implementation of the <code>VarLabel</code> interface. 
 */
public class VarLabel_c extends Label_c implements VarLabel {
    private final transient int uid = ++counter;
    private static int counter = 0;
    private String name;
    
    protected VarLabel_c() {
    }
    
    public VarLabel_c(String name, String description, JifTypeSystem ts, Position pos) {
        super(ts, pos);
        this.name = name;
        setDescription(description);
    }
    
    public boolean isEnumerable() { return true; }
    public boolean isComparable() { return false; }    
    public boolean isCanonical() { return true; }     
    public boolean isRuntimeRepresentable() { return false; }
    public boolean isCovariant() { return false; }
    
    public Set variables() { return Collections.singleton(this); }
    
    public String componentString() {
        if (Report.should_report(Report.debug, 2)) { 
            return "<var " + name + " " + uid + ">";
        }
        if (Report.should_report(Report.debug, 1)) { 
            return "<var " + name + ">";
        }
        return name;
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
    public Label subst(LocalInstance arg, Label l) {
        return this;
    }
    public Label subst(AccessPathRoot r, AccessPath e) {
        return this;
    }
}
