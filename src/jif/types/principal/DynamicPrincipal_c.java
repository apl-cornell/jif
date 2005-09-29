package jif.types.principal;

import java.util.List;

import jif.translate.DynamicPrincipalToJavaExpr_c;
import jif.types.*;
import jif.types.label.*;
import polyglot.main.Report;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** An implementation of the <code>DynamicPrincipal</code> interface. 
 */
public class DynamicPrincipal_c extends Principal_c implements DynamicPrincipal {
    private final AccessPath path;

    public DynamicPrincipal_c(AccessPath path, JifTypeSystem ts, Position pos) {
	super(ts, pos, new DynamicPrincipalToJavaExpr_c());
        this.path = path;
        if (path instanceof AccessPathConstant) {
            throw new InternalCompilerError("Don't expect to get AccessPathConstants for dynamic labels");
        }
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
        if (this == o) return true;
	if (! (o instanceof DynamicPrincipal)) {
	    return false;
	}

	DynamicPrincipal that = (DynamicPrincipal) o;
        return (this.path.equals(that.path()));
    }

    public int hashCode() {
	return path.hashCode();
    }
    
    public List throwTypes(TypeSystem ts) {
        return path.throwTypes(ts);
    }
    public Principal subst(AccessPathRoot r, AccessPath e) {
        AccessPath newPath = path.subst(r, e);
        if (newPath == path) {
            return this;
        }

        if (newPath instanceof AccessPathConstant) {
            AccessPathConstant apc = (AccessPathConstant)newPath;
            if (!apc.isPrincipalConstant()) {
                throw new InternalCompilerError("Replaced a dynamic principal with a non-principal!");
            }
            return (Principal)apc.constantValue();
        }
        
        return ((JifTypeSystem)typeSystem()).dynamicPrincipal(this.position(), newPath);
    }
    
    public PathMap labelCheck(JifContext A) {
        return path.labelcheck(A);
    }
    
}
