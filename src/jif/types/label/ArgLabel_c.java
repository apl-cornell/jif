package jif.types.label;

import java.util.Set;

import jif.types.JifTypeSystem;
import jif.types.hierarchy.LabelEnv;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.*;

/**
 * This label is used as the label of the real argument.
 * The purpose is to avoid having to re-interpret labels at each call.
 */
public class ArgLabel_c extends Label_c implements ArgLabel, LabelImpl {
    private final LocalInstance li;
    private Label upperBound;
    
    protected ArgLabel_c() {
        li = null;
    }
    public ArgLabel_c(JifTypeSystem ts, LocalInstance li, Position pos) {
        super(ts, pos);
        this.li = li;
    }
    
    public LocalInstance formalInstance() {
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
    public boolean isEnumerable() { return true; }
    public Set variables() { return ((LabelImpl)upperBound).variables(); }
    
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
    
    public String componentString() {
        if (Report.should_report(Report.debug, 2)) { 
            String ub = upperBound==null?"-":upperBound.toString();
            return "<arg " + li + " " + ub + ">";
        }
        else if (Report.should_report(Report.debug, 1)) {
            String ub = upperBound==null?"-":upperBound.toString();
            return "<arg " + li.name() + " " + ub + ">";
        }
        return li.name();
    }

    public boolean leq_(Label L, LabelEnv env) {
        // all we know about the arg label is that an upperbound is upperBound().
        // So the arg label is less than L if the upper bound is less than L.
        return env.leq(upperBound(), L);
    }

    public void translate(Resolver c, CodeWriter w) {
        throw new InternalCompilerError("Cannot translate \"" + this + "\".");
    }
}
