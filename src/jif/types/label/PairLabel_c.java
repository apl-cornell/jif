package jif.types.label;

import java.util.*;

import jif.translate.PairLabelToJavaExpr_c;
import jif.types.*;
import jif.types.hierarchy.LabelEnv;
import jif.visit.LabelChecker;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

public class PairLabel_c extends Label_c implements PairLabel {
    private final LabelJ labelJ;
    private final LabelM labelM;
    
    public PairLabel_c(JifTypeSystem ts, LabelJ labelJ, LabelM labelM, Position pos) {
        super(ts, pos, new PairLabelToJavaExpr_c());
        this.labelJ = labelJ;
        this.labelM = labelM;
        if (labelJ.isTop() && labelM.isTop()) {
            setDescription("Top of the label lattice, the most restrictive label possible");            
        }
        if (labelJ.isBottom() && labelM.isBottom()) {
            setDescription("Bottom of the label lattice, the least restrictive label possible");            
        }
    }
    
    public LabelJ labelJ() { return labelJ; }
    public LabelM labelM() { return labelM; }

    public boolean isRuntimeRepresentable() { 
        return labelJ.isRuntimeRepresentable() && labelM.isRuntimeRepresentable(); 
    }
    public boolean isCovariant() { return false; }    
    public boolean isComparable() { return true; }
    
    public boolean isEnumerable() { return true; }
    public boolean isCanonical() { 
        return labelJ.isCanonical() && labelM.isCanonical(); 
    }
    public boolean isDisambiguatedImpl() { return isCanonical(); }

    public boolean isTop() {
        return labelJ.isTop() && labelM.isTop();
    }
    public boolean isBottom() {
        return labelJ.isBottom() && labelM.isBottom();
    }
    
    public boolean equalsImpl(TypeObject o) {        
        if (this == o) return true;
        if (o instanceof PairLabel_c) {
            PairLabel_c that = (PairLabel_c)o;
            return this.labelJ.equals(that.labelJ) &&
                   this.labelM.equals(that.labelM);
        }
        return false;
    }
    
    public int hashCode() {
        return (this.labelJ.hashCode() ^ this.labelM.hashCode());
    }
    
    public boolean leq_(Label L, LabelEnv env, LabelEnv.SearchState state) {
        if (! L.isComparable()) {
            throw new InternalCompilerError("Cannot compare " + L);
        }
        if (! L.isSingleton()) {
            // only try to compare Policy labels against singletons.
            return false;
        }

        L = L.singletonComponent();
        
        if (L instanceof PairLabel) {
            PairLabel that = (PairLabel) L;
            return env.leq(this.labelJ, that.labelJ()) &&
                   env.leq(this.labelM, that.labelM());
        }        
        return false;
    }

    public String componentString(Set printedLabels) {
        StringBuffer sb = new StringBuffer();
        sb.append(this.labelJ.componentString(printedLabels));
        String s = this.labelM.componentString(printedLabels);
        if (s.length() > 0 && sb.length() > 0) {
            sb.append("; ");
        }
        sb.append(s);
        return sb.toString();
    }
    
    public List throwTypes(TypeSystem ts) {
        List l = new ArrayList(this.labelJ.throwTypes(ts));
        l.addAll(this.labelM.throwTypes(ts));
        return l;
    }

    public Label subst(LabelSubstitution substitution) throws SemanticException {
        PairLabel pl = this;
        LabelJ lj = this.labelJ.subst(substitution);
        LabelM lm = this.labelM.subst(substitution);
        if (lj != this.labelJ || lm != this.labelM) {
            pl = ((JifTypeSystem)ts).pairLabel(position(), lj, lm);
        }

        return substitution.substLabel(pl);
    }
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        A = (JifContext)A.pushBlock();
        PathMap X = this.labelJ.labelCheck(A, lc);                
        A.setPc(X.N());
        X = X.join(this.labelM.labelCheck(A, lc));            
        return X;
        
    }        
}
