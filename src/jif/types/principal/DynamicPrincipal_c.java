package jif.types.principal;

import jif.translate.DynamicPrincipalToJavaExpr_c;
import jif.types.JifTypeSystem;
import jif.types.label.AccessPath;
import polyglot.main.Report;
import polyglot.types.Resolver;
import polyglot.types.TypeObject;
import polyglot.util.Position;

/** An implementation of the <code>DynamicPrincipal</code> interface. 
 */
public class DynamicPrincipal_c extends Principal_c implements DynamicPrincipal {
    private final AccessPath path;

    public DynamicPrincipal_c(AccessPath path, JifTypeSystem ts, Position pos) {
	super(ts, pos, new DynamicPrincipalToJavaExpr_c());
        this.path = path;
    }

    public AccessPath path() {
	return path;
    }

    public boolean isRuntimeRepresentable() { return true; }
    public boolean isCanonical() { return path.isCanonical(); }

    public String toString() {
        if (Report.should_report(Report.debug, 1)) { 
            return "<pr-dynamic " + path + ">";
        }
        return path().toString();
    }


    public boolean equalsImpl(TypeObject o) {
	if (! (o instanceof DynamicPrincipal)) {
	    return false;
	}

	DynamicPrincipal that = (DynamicPrincipal) o;
        return (this.path.equals(that.path()));
    }

    public int hashCode() {
	return path.hashCode();
    }
    
    public String translate(Resolver c) {
        return path.translate(c);
    }
}
