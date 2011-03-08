package jif.types;

import java.util.*;

import jif.types.label.Label;
import jif.types.label.ParamLabel;
import jif.types.label.ThisLabel;
import jif.types.principal.ParamPrincipal;
import jif.types.principal.Principal;
import polyglot.ext.param.types.PClass;
import polyglot.frontend.Source;
import polyglot.types.ClassType;
import polyglot.types.FieldInstance;
import polyglot.types.LazyClassInitializer;
import polyglot.types.ParsedClassType_c;
import polyglot.types.TypeObject;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.TypedList;

/** An implementation of the <code>JifParsedPolyType</code> interface. 
 */
public class JifParsedPolyType_c extends ParsedClassType_c implements JifParsedPolyType
{
    List<ParamInstance> params;
    List<Principal> authority;
    List<ActsForConstraint<ActsForParam, Principal>> constraints;
    Label provider;
    
    PClass instantiatedFrom;

    protected JifParsedPolyType_c() {
	super();
	JifTypeSystem jts = (JifTypeSystem) this.ts;
	this.params = new LinkedList<ParamInstance>();
	this.authority = new LinkedList<Principal>();
	this.constraints = new LinkedList<ActsForConstraint<ActsForParam, Principal>>();
        this.provider = jts.providerLabel(position, this);
        this.instantiatedFrom = null;
    }

    public JifParsedPolyType_c(JifTypeSystem ts, LazyClassInitializer init, 
                               Source fromSource) {
	super(ts, init, fromSource);
        this.params = new LinkedList<ParamInstance>();
        this.authority = new LinkedList<Principal>();
        this.constraints = new LinkedList<ActsForConstraint<ActsForParam, Principal>>();
        this.provider = ts.providerLabel(position, this);
        this.instantiatedFrom = null;
    }

    @Override
    public PClass instantiatedFrom() {
        return instantiatedFrom;
    }

    @Override
    public void setInstantiatedFrom(PClass pc) {
        this.instantiatedFrom = pc;
    }

    @Override
    public void kind(Kind kind) {
        if (kind != TOP_LEVEL) {
            throw new InternalCompilerError("Jif does not support inner classes.");
        }
        super.kind(kind);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<FieldInstance> fields() {
        if (fields == null) {
            // initialize the fields list.
            super.fields();

            // Remove the class field.
            for (Iterator<FieldInstance> i = fields.iterator(); i.hasNext(); ) {
                FieldInstance fi = i.next();
                if (fi.name().equals("class")) {
                    i.remove();
                    break;
                }
            }
        }

        return fields;
    }

    @Override
    public List<Principal> authority() {
        return authority;
    }

    @Override
    public List<Principal> constructorCallAuthority() {
        List<Principal> l = new ArrayList<Principal>(authority.size());
        
        for (Principal p : authority) {
            if (p instanceof ParamPrincipal) {
                // p is a parameter principal.
                l.add(p);                            
            }
        }
        return l;
    }

    @Override
    public List<ParamInstance> params() {
	return params;
    }

    @Override
    public List<Param> actuals() {
        JifTypeSystem ts = (JifTypeSystem) this.ts;

        List<Param> actuals = new ArrayList<Param>(params.size());

        for (ParamInstance pi : params) {
            Position posi = pi.position();

            if (pi.isCovariantLabel()) {
                actuals.add(ts.covariantLabel(posi, pi));
            }
            else if (pi.isLabel()) {
                ParamLabel pl = ts.paramLabel(posi, pi);
                pl.setDescription("label parameter " + pi.name() + " of class " + pi.container().fullName());                
                actuals.add(pl);
            }
            else {
                actuals.add(ts.principalParam(posi, pi));
            }
        }

        return actuals;
    }

    @Override
    public ThisLabel thisLabel() {
	return ((JifTypeSystem)ts).thisLabel(this);
    }

    @Override
    public void addMemberClass(ClassType t) {
	throw new InternalCompilerError("Jif does not support inner classes.");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void setParams(List params) {
        this.params = new TypedList(params, ParamInstance.class, false);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void setAuthority(List principals) {
        this.authority = new TypedList(principals, Principal.class, false);
    }
    @Override
    public List<ActsForConstraint<ActsForParam, Principal>> constraints() {
    	return constraints;
    }
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void setConstraints(List constraints) {
        this.constraints = new TypedList(constraints, Constraint.class, false);
    }

    @Override
    public String toString() {
	String s = "";

        if (params != null) {
            for (Iterator<ParamInstance> i = params.iterator(); i.hasNext(); ) {
                ParamInstance pi = i.next();
                s += pi.toString();

                if (i.hasNext()) {
                    s += ", ";
                }
            }
        }

	if (! s.equals("")) {
	    s = "[" + s + "]";
	}

        if (package_() != null) {
	    return package_().toString() + "." + name + s;
	}

	return name + s;
    }

    @Override
    public int hashCode() {
        return flags.hashCode() + name.hashCode();
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (o instanceof JifPolyType) {
	    JifPolyType t = (JifPolyType) o;

	    if (package_() != null && t.package_() != null) {
		return package_().equals(t.package_())
		    && name.equals(t.name())
		    && flags.equals(t.flags())
		    && params.equals(t.params())
		    && authority.equals(t.authority());
	    }
	    else if (package_() == t.package_()) {
		return name.equals(t.name())
		    && flags.equals(t.flags())
		    && params.equals(t.params())
		    && authority.equals(t.authority());
	    }
	}

	return false;
    }

    @Override
    public Label provider() {
        return provider;
    }
}
