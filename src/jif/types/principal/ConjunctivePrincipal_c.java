package jif.types.principal;

import java.util.*;

import jif.translate.ConjunctivePrincipalToJavaExpr_c;
import jif.types.JifTypeSystem;
import polyglot.main.Report;
import polyglot.types.TypeObject;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

public class ConjunctivePrincipal_c extends Principal_c implements ConjunctivePrincipal {
    private final Set conjuncts;
    public ConjunctivePrincipal_c(Collection conjuncts, 
                                  JifTypeSystem ts, Position pos) {
        super(ts, pos, new ConjunctivePrincipalToJavaExpr_c());
        this.conjuncts = new LinkedHashSet(conjuncts);
        if (conjuncts.size() < 2) {
            throw new InternalCompilerError("ConjunctivePrincipal should " +
                        "have at least 2 members");
        }
    }
    
    public boolean isRuntimeRepresentable() {
        for (Iterator iter = conjuncts.iterator(); iter.hasNext(); ) {
            Principal p = (Principal)iter.next();
            if (!p.isRuntimeRepresentable()) return false;
        }
        return true;
    }
    public boolean isCanonical() { 
        for (Iterator iter = conjuncts.iterator(); iter.hasNext(); ) {
            Principal p = (Principal)iter.next();
            if (!p.isCanonical()) return false;
        }
        return true;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        String sep = "&";
        if (Report.should_report(Report.debug, 1)) {
            sb.append("<");
            sep = " and ";
        }
        else if (Report.should_report(Report.debug, 2)) {
            sb.append("<conjunction: ");
            sep = " and ";
        }
        for (Iterator iter = conjuncts.iterator(); iter.hasNext(); ) {
            Principal p = (Principal)iter.next(); 
            if (p instanceof DisjunctivePrincipal) {
                sb.append('(');
                sb.append(p);
                sb.append(')');
            }
            else {
                sb.append(p);            
            }
            if (iter.hasNext()) sb.append(sep);
        }
        
        if (Report.should_report(Report.debug, 1)) {
            sb.append(">");
        }
        else if (Report.should_report(Report.debug, 2)) {
            sb.append(">");
        }
        return sb.toString();
    }
    
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (o instanceof ConjunctivePrincipal) {
            ConjunctivePrincipal that = (ConjunctivePrincipal)o;
            return this.conjuncts.equals(that.conjuncts());
        }
        return false;
    }
    
    public int hashCode() {
        return conjuncts.hashCode();
    }

    public Set conjuncts() {
        return conjuncts;
    }
}
