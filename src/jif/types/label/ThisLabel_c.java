package jif.types.label;


import jif.types.*;
import jif.types.hierarchy.LabelEnv;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.*;

/** An implementation of the <code>ParamLabel</code> interface. 
 */
public class ThisLabel_c extends ParamLabel_c implements ThisLabel {
    private final JifClassType ct;
    public ThisLabel_c(JifClassType ct, JifTypeSystem ts, Position pos) {
        super(ts.paramInstance(pos, ct, ParamInstance.INVARIANT_LABEL, "this"), ts, pos);
        this.ct = ct;
    }
    
    public JifClassType classType() {
        return ct;
    }

    public boolean isRuntimeRepresentable() { return false; }
    public boolean isCovariant() { return false; }
    public boolean isComparable() { return true; }
    public boolean isCanonical() { return true; }
    public boolean isEnumerable() { return true; }
    public int hashCode() {
        return ct.hashCode();
    }
    public boolean equalsImpl(TypeObject o) {
        if (! (o instanceof ThisLabel)) {
            return false;
        }           
        ThisLabel that = (ThisLabel) o;
        return (this.ct.equals(that.classType()));
    }
    
    public String componentString() {
        if (Report.should_report(Report.debug, 2)) { 
            return "<this-label " + this.ct.fullName() + ">";
        }
        if (Report.should_report(Report.debug, 1)) { 
            return "<this-label>";
        }
        return "this";
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
