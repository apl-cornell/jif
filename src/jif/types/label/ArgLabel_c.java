package jif.types.label;

import java.util.Set;

import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import jif.types.hierarchy.LabelEnv;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.Position;

/**
 * This label is used as the label of the real argument.
 * The purpose is to avoid having to re-interpret labels at each call.
 */
public class ArgLabel_c extends Label_c implements ArgLabel {
    private final VarInstance vi;
    private Label upperBound;
    
    protected ArgLabel_c() {
        vi = null;
    }
    public ArgLabel_c(JifTypeSystem ts, VarInstance vi, Position pos) {
        super(ts, pos);
        this.vi = vi;
    }
    
    public VarInstance formalInstance() {
        return vi;
    }
    
    public Label upperBound() {
        return upperBound;
    }
    
    public void setUpperBound(Label upperBound) {
        this.upperBound = upperBound;
    }

    public boolean isRuntimeRepresentable() { return false; }
    public boolean isCovariant() { return false; }
    public boolean isComparable() { return true; }
    public boolean isCanonical() { return true; }
    public boolean isEnumerable() { return true; }
    protected boolean isDisambiguatedImpl() { return upperBound != null; }
    
    public boolean equalsImpl(TypeObject o) {
        if (! (o instanceof ArgLabel_c)) {
            return false;
        }           
        ArgLabel_c that = (ArgLabel_c) o;
        return (this.vi.equals(that.vi));
    }
    public int hashCode() {
        return vi.hashCode();
    }
        
    public String componentString(Set printedLabels) {
        if (printedLabels.contains(this)) {
            if (Report.should_report(Report.debug, 2)) { 
                return "<arg " + vi.name() + ">";
            }
            else if (Report.should_report(Report.debug, 1)) {
                return "<arg " + vi.name() + ">";
            }
            return vi.name();            
        }
        printedLabels.add(this);
        
        if (Report.should_report(Report.debug, 2)) { 
            String ub = upperBound==null?"-":upperBound.toString(printedLabels);
            return "<arg " + vi.name() + " " + ub + ">";
        }
        else if (Report.should_report(Report.debug, 1)) {
            String ub = upperBound==null?"-":upperBound.toString(printedLabels);
            return "<arg " + vi.name() + " " + ub + ">";
        }
        return vi.name();
    }

    public boolean leq_(Label L, LabelEnv env, LabelEnv.SearchState state) {
        // Should not recurse here, but allow the Label Env to do the recursion
        // on upperBound(), to avoid infinite loops.
        return false;
    }
 
    public Label subst(LabelSubstitution substitution) throws SemanticException {
        ArgLabel lbl = this;
        if (!substitution.stackContains(this)) {
            substitution.pushLabel(this);
            Label newBound = lbl.upperBound().subst(substitution);
            
            if (newBound != lbl.upperBound()) {
                JifTypeSystem ts = (JifTypeSystem)typeSystem();
                lbl = ts.argLabel(lbl.position(), lbl.formalInstance());
                lbl.setUpperBound(newBound);
            }
            substitution.popLabel(this);
        }
        else {
            // the stack already contains this label, so don't call the 
            // substitution recursively
        }
        return substitution.substLabel(lbl);

    }
    
    
}
