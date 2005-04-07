package jif.types.label;


import java.util.Set;

import jif.types.JifTypeSystem;
import jif.types.ParamInstance;
import jif.types.hierarchy.LabelEnv;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.types.Resolver;
import polyglot.types.TypeObject;
import polyglot.util.*;

/** An implementation of the <code>ParamLabel</code> interface. 
 */
public class ParamLabel_c extends Label_c implements ParamLabel {
    private final ParamInstance paramInstance;
    public ParamLabel_c(ParamInstance paramInstance, JifTypeSystem ts, Position pos) {
        super(ts, pos);
        this.paramInstance = paramInstance;
    }
    
    public ParamInstance paramInstance() {
        return paramInstance;
    }

    public boolean isRuntimeRepresentable() { return false; }
    public boolean isCovariant() { return false; }
    public boolean isComparable() { return true; }
    public boolean isCanonical() { return paramInstance.isCanonical(); }
    public boolean isDisambiguated() { return isCanonical(); }
    public boolean isEnumerable() { return true; }
    public int hashCode() {
        return paramInstance.hashCode();
    }
    public boolean equalsImpl(TypeObject o) {
        if (! (o instanceof ParamLabel)) {
            return false;
        }           
        ParamLabel that = (ParamLabel) o;
        return (this.paramInstance == that.paramInstance());
    }
    
    public String componentString(Set printedLabels) {
        if (Report.should_report(Report.debug, 1)) { 
            return "<param-label " + this.paramInstance + ">";
        }
        return "<param-label " + this.paramInstance.name() + ">";
    }

    public boolean leq_(Label L, LabelEnv env) {
        // only leq if equal to this parameter, which is checked before 
        // this method is called.
        return false;
    }
    public void translate(Resolver c, CodeWriter w) {
        throw new InternalCompilerError("Cannot translate " + this);
    }
    public Label subst(LocalInstance arg, Label l) {
        return this;
    }
    public Label subst(AccessPathRoot r, AccessPath e) {
        return this;
    }
}
