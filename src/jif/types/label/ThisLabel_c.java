package jif.types.label;

import java.util.Set;

import jif.types.*;
import jif.types.hierarchy.LabelEnv;
import polyglot.main.Report;
import polyglot.types.SemanticException;
import polyglot.types.TypeObject;
import polyglot.util.Position;

public class ThisLabel_c extends Label_c implements ThisLabel {
    private final JifClassType ct;
    private final String fullName;
    
    public ThisLabel_c(JifTypeSystem ts, JifClassType ct, Position pos) {
        super(ts, pos);
        this.ct = ct;
        this.fullName = ct.fullName();
    }
    

    public boolean isRuntimeRepresentable() { return false; }
    public boolean isCovariant() { return true; }
    public boolean isComparable() { return true; }
    public boolean isCanonical() { return true; }
    public boolean isDisambiguated() { return true; }
    public boolean isEnumerable() { return true; }
    
    public JifClassType classType() {
        return ct;
    }
    
    public boolean equalsImpl(TypeObject o) {
        if (! (o instanceof ThisLabel)) {
            return false;
        }           
        ThisLabel that = (ThisLabel) o;
        return this.ct.equals(that.classType());
    }
    public int hashCode() {
        return fullName.hashCode();
    }
    
    public String componentString(Set printedLabels) {
        if (Report.should_report(Report.debug, 2)) { 
            return "<this (of " + ct.fullName() + ">";
        }
        else if (Report.should_report(Report.debug, 1)) {
            return "<this (of " + ct.name() + ">";
        }
        return "this";            
    }

    public boolean leq_(Label L, LabelEnv env) {
        // We know nothing about the this label, save that it is equal to itself,
        // and whatever is in the environment.
        return false;
    }
 
    public Label subst(LabelSubstitution substitution) throws SemanticException {
        return substitution.substLabel(this);
    }
    
    
}
