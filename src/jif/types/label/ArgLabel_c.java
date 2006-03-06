package jif.types.label;

import java.util.Set;

import jif.types.JifProcedureInstance;
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
    private CodeInstance ci; // code instance containing vi, if relevant
    private final String name;
    private Label upperBound;
    
    
    protected ArgLabel_c() {
        vi = null;
        ci = null;
        name =  null;
    }
    public ArgLabel_c(JifTypeSystem ts, VarInstance vi, CodeInstance ci, Position pos) {
        super(ts, pos);        
        this.vi = vi;
        this.ci = ci;
        this.name = vi.name();
        setDescription();
    }

    public ArgLabel_c(JifTypeSystem ts, ProcedureInstance pi, String name, Position pos) {
        super(ts, pos);        
        this.vi = null;
        this.ci = pi;
        this.name = name;
    }
    
    private void setDescription() {
        if (vi != null) {
            StringBuffer sb = new StringBuffer();
            sb.append("polymorphic label of the formal argument ");
            sb.append(vi.name());
            if (ci instanceof JifProcedureInstance) {
                sb.append(" of ");
                sb.append(((JifProcedureInstance)ci).debugString());
            }
            sb.append(" (bounded above by ");
            sb.append(upperBound);
            sb.append(")");
            
            this.setDescription(sb.toString());
        }
    }
    public VarInstance formalInstance() {
        return vi;
    }
    
    public Label upperBound() {
        return upperBound;
    }
    
    public void setUpperBound(Label upperBound) {
        this.upperBound = upperBound;
        setDescription();
    }

    public void setCodeInstance(CodeInstance ci) {
        this.ci = ci;
    }

    public boolean isRuntimeRepresentable() { return false; }
    public boolean isCovariant() { return false; }
    public boolean isComparable() { return true; }
    public boolean isCanonical() { return true; }
    public boolean isEnumerable() { return true; }
    protected boolean isDisambiguatedImpl() { return upperBound != null; }
    
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (! (o instanceof ArgLabel_c)) {
            return false;
        }           
        ArgLabel_c that = (ArgLabel_c) o;
        return (this.ci == that.ci || (this.ci != null && this.ci.equals(that.ci))) &&
               (this.vi == that.vi || (this.vi != null && this.vi.equals(that.vi)));
    }
    public int hashCode() {
        return (vi==null?234:vi.hashCode()) ^ 2346882;
    }
        
    public String componentString(Set printedLabels) {
        if (printedLabels.contains(this)) {
            if (Report.should_report(Report.debug, 2)) { 
                return "<arg " + name + ">";
            }
            else if (Report.should_report(Report.debug, 1)) {
                return "<arg " + name + ">";
            }
            return name;            
        }
        printedLabels.add(this);
        
        if (Report.should_report(Report.debug, 2)) { 
            String ub = upperBound==null?"-":upperBound.toString(printedLabels);
            return "<arg " + name + " " + ub + ">";
        }
        else if (Report.should_report(Report.debug, 1)) {
            String ub = upperBound==null?"-":upperBound.toString(printedLabels);
            return "<arg " + name + " " + ub + ">";
        }
        return name;
    }

    public boolean leq_(Label L, LabelEnv env, LabelEnv.SearchState state) {
        // Should not recurse here, but allow the Label Env to do the recursion
        // on upperBound(), to avoid infinite loops.
        return false;
    }
 
    public Label subst(LabelSubstitution substitution) throws SemanticException {
        ArgLabel lbl = this;
        if (substitution.recurseIntoChildren(lbl)) {
            if (!substitution.stackContains(this)) {
                substitution.pushLabel(this);
                Label newBound = lbl.upperBound().subst(substitution);
                
                if (newBound != lbl.upperBound()) {
                    lbl = (ArgLabel)lbl.copy();
                    lbl.setUpperBound(newBound);
                }
                substitution.popLabel(this);
            }
            else {
                // the stack already contains this label, so don't call the 
                // substitution recursively
            }
        }
        return substitution.substLabel(lbl);

    }
    public String description() {
        setDescription();
        return super.description();
    }
    
    
}
