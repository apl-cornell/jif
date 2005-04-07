package jif.types;

import java.util.*;

import jif.types.label.*;
import jif.types.label.Label;
import jif.types.label.ParamLabel;
import jif.types.principal.ParamPrincipal;
import jif.types.principal.Principal;
import polyglot.ext.jl.types.ParsedClassType_c;
import polyglot.ext.param.types.PClass;
import polyglot.frontend.Source;
import polyglot.types.*;
import polyglot.util.*;

/** An implementation of the <code>JifParsedPolyType</code> interface. 
 */
public class JifParsedPolyType_c extends ParsedClassType_c implements JifParsedPolyType
{
    Label thisLabel;
    List params;
    List authority;
    boolean invariant;
    PClass instantiatedFrom;

    protected JifParsedPolyType_c() {
	super();
	this.params = new TypedList(new LinkedList(), ParamInstance.class, false);
	this.authority = new TypedList(new LinkedList(), Principal.class, false);
	this.invariant = false;
        this.instantiatedFrom = null;
    }

    public JifParsedPolyType_c(JifTypeSystem ts, LazyClassInitializer init, 
                               Source fromSource) {
	super(ts, init, fromSource);
	this.params = new TypedList(new LinkedList(), ParamInstance.class, false);
	this.authority = new TypedList(new LinkedList(), Principal.class, false);
        this.instantiatedFrom = null;
        this.thisLabel = ts.unknownLabel(this.position()); 
    }

    public PClass instantiatedFrom() {
        return instantiatedFrom;
    }

    public void setInstantiatedFrom(PClass pc) {
        this.instantiatedFrom = pc;
    }

    public void kind(Kind kind) {
        if (kind != TOP_LEVEL) {
            throw new InternalCompilerError("Jif does not support inner classes.");
        }
        super.kind(kind);
    }

    public List fields() {
        if (fields == null) {
            // initialize the fields list.
            super.fields();

            // Remove the class field.
            for (Iterator i = fields.iterator(); i.hasNext(); ) {
                FieldInstance fi = (FieldInstance) i.next();
                if (fi.name().equals("class")) {
                    i.remove();
                    break;
                }
            }
        }

        return fields;
    }

    public List authority() {
    return authority;
    }

    public List constructorCallAuthority() {
        List l = new ArrayList(authority.size());
        
        Iterator iter = authority.iterator();
        
        while (iter.hasNext()) {
            Principal p = (Principal)iter.next();
            if (p instanceof ParamPrincipal) {
                // p is a parameter principal.
                l.add(p);                            
            }
        }
        return l;
    }

    public List params() {
	return params;
    }

    public List actuals() {
        JifTypeSystem ts = (JifTypeSystem) this.ts;

        List actuals = new ArrayList(params.size());

        for (Iterator i = params.iterator(); i.hasNext(); ) {
            ParamInstance pi = (ParamInstance) i.next();
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

    public Label thisLabel() {
	return this.thisLabel;
    }

    public void setThisLabel(Label L) {
	this.thisLabel = L;
    }

    public boolean isInvariant() {
	return this.invariant;
    }

    public void setInvariant(boolean inv) {
	this.invariant = inv;
    }
    
    public void addMemberClass(ClassType t) {
	throw new InternalCompilerError("Jif does not support inner classes.");
    }

    public void setParams(List params) {
        this.params = new TypedList(params, ParamInstance.class, false);
    }

    public void setAuthority(List principals) {
        this.authority = new TypedList(principals, Principal.class, false);
    }

    public String toString() {
	String s = "";

        if (params != null) {
            for (Iterator i = params.iterator(); i.hasNext(); ) {
                ParamInstance pi = (ParamInstance) i.next();
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
	    return package_().toString() + "." + name + s +
		(invariant? "(invariant)" : "");
	}

	return name + s;
    }

    public int hashCode() {
        return flags.hashCode() + name.hashCode();
    }

    public boolean equalsImpl(TypeObject o) {
        if (o instanceof JifPolyType) {
	    JifPolyType t = (JifPolyType) o;

	    if (package_() != null && t.package_() != null) {
		return package_().equals(t.package_())
		    && name.equals(t.name())
		    && flags.equals(t.flags())
		    && params.equals(t.params())
		    && authority.equals(t.authority())
                    && invariant == t.isInvariant();
	    }
	    else if (package_() == t.package_()) {
		return name.equals(t.name())
		    && flags.equals(t.flags())
		    && params.equals(t.params())
		    && authority.equals(t.authority())
                    && invariant == t.isInvariant();
	    }
	}

	return false;
    }

    public JifClassType setInvariantThis(Label L) throws SemanticException {
	JifTypeSystem jts = (JifTypeSystem) typeSystem();

        if (! (thisLabel instanceof ThisLabel)) {
            throw new SemanticException("Cannot set invariant this label.");
        }

        Map subst = new HashMap();
        subst.put(((ThisLabel)thisLabel).paramInstance(), L);

        return (JifClassType) jts.subst(this, subst);
    }

    /*
    public ClassType nullInstantiate(Position pos) {
	JifTypeSystem jts = (JifTypeSystem) typeSystem();
        return jts.nullInstantiate(pos, this);
    }
    */
}
