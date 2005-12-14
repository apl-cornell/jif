package jif.types.principal;

import jif.translate.DisjunctivePrincipalToJavaExpr_c;
import jif.types.JifTypeSystem;
import polyglot.main.Report;
import polyglot.types.TypeObject;
import polyglot.util.Position;

public class DisjunctivePrincipal_c extends Principal_c implements DisjunctivePrincipal {
    private final Principal disjunctL;
    private final Principal disjunctR;
    public DisjunctivePrincipal_c(Principal disjunctL, Principal disjunctR, 
                                  JifTypeSystem ts, Position pos) {
        super(ts, pos, new DisjunctivePrincipalToJavaExpr_c());
        this.disjunctL = disjunctL;
        this.disjunctR = disjunctR;
    }
    
    public boolean isRuntimeRepresentable() { 
        return disjunctL.isRuntimeRepresentable() && disjunctR.isRuntimeRepresentable(); 
    }
    public boolean isCanonical() { 
        return disjunctL.isCanonical() && disjunctR.isCanonical(); 
    }
    
    public String toString() {
        if (Report.should_report(Report.debug, 1)) {
            return "<" + disjunctL + " or " + disjunctR+ ">";
        }
        else if (Report.should_report(Report.debug, 2)) {
            return "<disjunction: " + disjunctL + " or " + disjunctR+ ">";
        }
        return disjunctL + "," + disjunctR;
    }
    
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (o instanceof DisjunctivePrincipal) {
            DisjunctivePrincipal that = (DisjunctivePrincipal)o;
            return this.disjunctL.equals(that.disjunctLeft()) &&
                this.disjunctR.equals(that.disjunctRight());
        }
        return false;
    }
    
    public int hashCode() {
        return disjunctL.hashCode() ^ disjunctR.hashCode() ^ 23789;
    }

    public Principal disjunctLeft() {
        return disjunctL;
    }

    public Principal disjunctRight() {
        return disjunctR;
    }
}
