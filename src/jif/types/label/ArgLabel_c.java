package jif.types.label;

import java.util.Set;

import jif.types.JifLocalInstance;
import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import jif.types.hierarchy.LabelEnv;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/**
 * This label is used as the label of the real argument.
 * The purpose is to avoid having to re-interpret labels at each call.
 */
public class ArgLabel_c extends Label_c implements ArgLabel {
    private final JifLocalInstance li;
    private Label upperBound;
    
    protected ArgLabel_c() {
        li = null;
    }
    public ArgLabel_c(JifTypeSystem ts, JifLocalInstance li, Position pos) {
        super(ts, pos);
        this.li = li;
    }
    
    public JifLocalInstance formalInstance() {
        return li;
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
    public boolean isDisambiguated() { return upperBound != null && upperBound.isDisambiguated(); }
    public boolean isEnumerable() { return true; }
    public Set variables() { return upperBound.variables(); }
    
    public boolean equalsImpl(TypeObject o) {
        if (! (o instanceof ArgLabel)) {
            return false;
        }           
        ArgLabel that = (ArgLabel) o;
        return (this.li == that.formalInstance());
    }
    public int hashCode() {
        return li.hashCode();
    }
    
    public String componentString(Set printedLabels) {
        if (printedLabels.contains(this)) {
            if (Report.should_report(Report.debug, 2)) { 
                return "<arg " + li.name() + ">";
            }
            else if (Report.should_report(Report.debug, 1)) {
                return "<arg " + li.name() + ">";
            }
            return li.name();            
        }
        printedLabels.add(this);
        
        if (Report.should_report(Report.debug, 2)) { 
            String ub = upperBound==null?"-":upperBound.toString(printedLabels);
            return "<arg " + li.name() + " " + ub + ">";
        }
        else if (Report.should_report(Report.debug, 1)) {
            String ub = upperBound==null?"-":upperBound.toString(printedLabels);
            return "<arg " + li.name() + " " + ub + ">";
        }
        return li.name();
    }

    public boolean leq_(Label L, LabelEnv env) {
        // all we know about the arg label is that an upperbound is upperBound().
        // So the arg label is less than L if the upper bound is less than L.
        return env.leq(upperBound(), L);
    }
 
    public Label subst(LocalInstance arg, Label l) {
        if (this.li.equals(arg)) {
            return l;
        }
        return this;
    }
    public Label subst(AccessPathRoot r, AccessPath e) {
        return this;
    }
    public Label subst(LabelSubstitution substitution) throws SemanticException {
        Label newBound = upperBound.subst(substitution);
        
        if (newBound != upperBound) {
            JifTypeSystem ts = (JifTypeSystem)typeSystem();
            ArgLabel newLabel = ts.argLabel(this.position(), li);
            newLabel.setUpperBound(newBound);
            return substitution.substLabel(newLabel);        
        }
        else {
            return substitution.substLabel(this);
        }
    }
    
    
}
