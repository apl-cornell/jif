package jif.types.principal;

import jif.types.*;
import jif.types.JifTypeSystem;
import jif.types.ParamInstance;
import jif.types.label.*;
import jif.types.label.AccessPath;
import jif.types.label.AccessPathRoot;
import jif.types.label.Label;
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
    public boolean isRuntimeRepresentable() { return true; }
    public boolean isCanonical() { return true; }

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
    
    public PathMap labelCheck(JifContext A) {
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
        Label l;
        if (A.inStaticContext()) {
            // return a special arg label
            ArgLabel al = ts.argLabel(this.position, paramInstance); 
            if (A.inConstructorCall()) {
                al.setUpperBound(ts.thisLabel(this.position(), (JifClassType)A.currentClass()));
            }
            else {
                al.setUpperBound(ts.topLabel());
            }
            l = al;
        }
        else {
            l = ts.thisLabel(this.position(), (JifClassType)A.currentClass());
        }
        return ts.pathMap().N(l).NV(l);
    }
    
}
