package jif.types.label;

import java.util.*;

import jif.translate.PairLabelToJavaExpr_c;
import jif.types.*;
import jif.types.hierarchy.LabelEnv;
import jif.visit.LabelChecker;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.Position;

public class PairLabel_c extends Label_c implements PairLabel {
    private final ConfPolicy confPolicy;
    private final IntegPolicy integPolicy;
    
    public PairLabel_c(JifTypeSystem ts, 
                       ConfPolicy confPolicy, 
                       IntegPolicy integPolicy, 
                       Position pos) {
        super(ts, pos, new PairLabelToJavaExpr_c());        
        this.confPolicy = confPolicy;
        this.integPolicy = integPolicy;        
    }
    
    public ConfPolicy confPolicy() {
        return this.confPolicy;
    }
    public IntegPolicy integPolicy() {
        return this.integPolicy;
    }
    
    public boolean isRuntimeRepresentable() { 
        return confPolicy.isRuntimeRepresentable() && 
               integPolicy.isRuntimeRepresentable(); 
    }
    public boolean isCovariant() { return false; }
    public boolean isComparable() { return true; }
    public boolean isCanonical() { 
        return confPolicy.isCanonical() && 
               integPolicy.isCanonical();         
    }
    public boolean isEnumerable() { return true; }
    protected boolean isDisambiguatedImpl() { return isCanonical(); }
    
    public boolean isBottom() {
        return confPolicy.isBottom() && integPolicy.isBottom();
    }

    public boolean isTop() {
        return confPolicy.isTop() && integPolicy.isTop();
    }
    
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (! (o instanceof PairLabel_c)) {
            return false;
        }           
        PairLabel_c that = (PairLabel_c) o;
        return (this.confPolicy.equals(that.confPolicy)) &&
               (this.integPolicy.equals(that.integPolicy));
    }
    
    protected Label simplifyImpl() {
        ConfPolicy cp = (ConfPolicy)confPolicy.simplify();
        IntegPolicy ip = (IntegPolicy)integPolicy.simplify();
        if (cp != confPolicy || ip != integPolicy) {
            return ((JifTypeSystem)ts).pairLabel(position, cp, ip);
        }
        return this;
    }

    public ConfPolicy confProjection() {
        return confPolicy();
    }
    public IntegPolicy integProjection() {
        return integPolicy();
    }
    
    public int hashCode() {
        return confPolicy.hashCode() ^ integPolicy.hashCode();
    }
        
    public String toString() {
        return toString(true);        
    }
    
    public String componentString(Set printedLabels) {
        return toString(false);
    }
    public String toString(boolean topLevel) {
        StringBuffer sb = new StringBuffer();
        if (topLevel) sb.append("{");
        if (Report.should_report(Report.debug, 2)) { 
            sb.append("<pair " + confPolicy.toString() + " ; " + integPolicy.toString() + ">");
        }
        else if (Report.should_report(Report.debug, 1)) { 
            sb.append(confPolicy.toString() + "; " + integPolicy.toString());
        }
        else {
            String cs = "";
            if (!topLevel || !confPolicy.isBottomConfidentiality()) {
                cs = confPolicy.toString();
            }
            String is = "";
            if (!topLevel || !integPolicy.isTopIntegrity()) {
                is = integPolicy.toString();
            }
            if (cs.length() > 0 && is.length() > 0) {
                sb.append(cs + "; " + is);
            }
            else {
                sb.append(cs + is);
            }
        }
        if (topLevel) sb.append("}");
        return sb.toString();
    }

    public boolean leq_(Label L, LabelEnv env, LabelEnv.SearchState state) {
        if (L instanceof PairLabel) {
            PairLabel that = (PairLabel)L;
//            System.out.println("***Comparing " + this + " to " + that);
//            System.out.println("   to wit " + this.confPolicy() + " to " + that.confPolicy());
//            System.out.println("   and " + this.integPolicy() + " to " + that.integPolicy());
//            System.out.println("   " + env.leq(this.confPolicy(), that.confPolicy()));
//            System.out.println("   " + env.leq(this.integPolicy(), that.integPolicy()));
            return env.leq(this.confPolicy(), that.confPolicy()) &&
                   env.leq(this.integPolicy(), that.integPolicy());
        }
        return false;
    }
 
    public Label subst(LabelSubstitution substitution) throws SemanticException {
        PairLabel lbl = this;
        ConfPolicy newCP = (ConfPolicy)lbl.confPolicy().subst(substitution);
        IntegPolicy newIP = (IntegPolicy)lbl.integPolicy().subst(substitution);
        
        if (newCP != this.confPolicy || newIP != this.integPolicy) {
            lbl = ((JifTypeSystem)ts).pairLabel(position, newCP, newIP);
        }
        return substitution.substLabel(lbl);
    }
    
    public List throwTypes(TypeSystem ts) {
        List throwTypes = new ArrayList();
        throwTypes.addAll(confPolicy.throwTypes(ts));
        throwTypes.addAll(integPolicy.throwTypes(ts));
        return throwTypes; 
    }
    
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
        PathMap X = ts.pathMap().N(A.pc()).NV(A.pc());
        

        A = (JifContext)A.pushBlock();
        
        PathMap Xc = confPolicy.labelCheck(A, lc);
        X = X.join(Xc);            
        
        A.setPc(X.N());
        PathMap Xi = integPolicy.labelCheck(A, lc);
        X = X.join(Xi);            
        return X;
    }    
}
