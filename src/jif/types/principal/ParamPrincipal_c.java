package jif.types.principal;

import jif.types.JifTypeSystem;
import jif.types.ParamInstance;
import jif.types.label.AccessPath;
import jif.types.label.AccessPathRoot;
import polyglot.main.Report;
import polyglot.types.Resolver;
import polyglot.types.TypeObject;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** An implementation of the <code>ParamPrincipal</code> interface. 
 */
public class ParamPrincipal_c extends Principal_c implements ParamPrincipal {
    private final ParamInstance paramInstance;
    public ParamPrincipal_c(ParamInstance paramInstance, JifTypeSystem ts, Position pos) {
	super(ts, pos);
	this.paramInstance = paramInstance;
    }

    public ParamInstance paramInstance() {
        return paramInstance;
    }
    public boolean isRuntimeRepresentable() { return false; }
    public boolean isCanonical() { return false; }

    public String toString() {
        if (Report.should_report(Report.debug, 1)) { 
            return "<pr-param " + paramInstance + ">";
        }
        return paramInstance.name();
    }

    public boolean equalsImpl(TypeObject o) {
	if (! (o instanceof ParamPrincipal)) {
	    return false;
	}

	ParamPrincipal that = (ParamPrincipal) o;
	return this.paramInstance.equals(that.paramInstance());
    }

    public int hashCode() {
	return paramInstance.hashCode();
    }

    public String translate(Resolver c) {
        throw new InternalCompilerError("Cannot translate principal \"" + this + "\".");
    }
    public Principal subst(AccessPathRoot r, AccessPath e) {
        return this;
    }
}
