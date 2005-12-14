package jif.types.principal;

import jif.translate.CannotPrincipalToJavaExpr_c;
import jif.translate.ConjunctivePrincipalToJavaExpr_c;
import jif.types.JifTypeSystem;
import polyglot.main.Report;
import polyglot.types.TypeObject;
import polyglot.util.Position;

public class ConjunctivePrincipal_c extends Principal_c implements ConjunctivePrincipal {
    private final Principal conjunctL;
    private final Principal conjunctR;
    public ConjunctivePrincipal_c(Principal conjunctL, Principal conjunctR, 
                                  JifTypeSystem ts, Position pos) {
        super(ts, pos, new ConjunctivePrincipalToJavaExpr_c());
        this.conjunctL = conjunctL;
        this.conjunctR = conjunctR;
    }
    
    public boolean isRuntimeRepresentable() { 
        return conjunctL.isRuntimeRepresentable() && conjunctR.isRuntimeRepresentable(); 
    }
    public boolean isCanonical() { 
        return conjunctL.isCanonical() && conjunctR.isCanonical(); 
    }
    
    public String toString() {
        if (Report.should_report(Report.debug, 1)) {
            return "<" + conjunctL + " and " + conjunctR+ ">";
        }
        else if (Report.should_report(Report.debug, 2)) {
            return "<conjunction: " + conjunctL + " and " + conjunctR+ ">";
        }
        StringBuffer sb = new StringBuffer();
        if (conjunctL instanceof DisjunctivePrincipal) {
            sb.append('(');
            sb.append(conjunctL);
            sb.append(')');
        }
        else {
            sb.append(conjunctL);            
        }
        sb.append('&');
        if (conjunctR instanceof DisjunctivePrincipal) {
            sb.append('(');
            sb.append(conjunctR);
            sb.append(')');
        }
        else {
            sb.append(conjunctR);            
        }
        
        return sb.toString();
    }
    
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (o instanceof ConjunctivePrincipal) {
            ConjunctivePrincipal that = (ConjunctivePrincipal)o;
            return this.conjunctL.equals(that.conjunctLeft()) &&
                this.conjunctR.equals(that.conjunctRight());
        }
        return false;
    }
    
    public int hashCode() {
        return conjunctL.hashCode() ^ conjunctR.hashCode() ^ 23789;
    }

    public Principal conjunctLeft() {
        return conjunctL;
    }

    public Principal conjunctRight() {
        return conjunctR;
    }
}
