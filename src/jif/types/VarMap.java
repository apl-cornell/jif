package jif.types;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jif.types.label.*;
import jif.types.label.DynamicLabel;
import jif.types.label.Label;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;

import polyglot.types.ArrayType;
import polyglot.types.Type;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;

/** 
 * Maps variable components to labels. Whether these labels are interpreted 
 * as upper bounds, lower bounds, or just labels depends on the use of the
 * VarMap. 
 * 
 * If a variable v is in the map when boundOf(v) is called, boundOf(v) will 
 * return the label defaultBound, and enter the mapping v->defaultBound into
 * the map, so that next time boundOf(v) is called, the same label will be
 * returned, even if the default bound has changed in the meantime, through
 * the setDefaultBound(Label) method.
 * 
 * The defaultLabel defaults to ts.topLabel().
 */
public class VarMap {
    private Map bounds;
    private JifTypeSystem ts;
    private Label defaultBound;

    public VarMap(JifTypeSystem ts) {
	this.ts = ts;
	this.bounds = new HashMap();
        this.defaultBound = ts.topLabel();
    }
    public VarMap(JifTypeSystem ts, Label defaultBound) {
	this.ts = ts;
	this.bounds = new HashMap();
        this.defaultBound = defaultBound;
        if (defaultBound == null) {
            throw new InternalCompilerError("defaultBound cannot be null");
        }
    }

    protected VarMap(JifTypeSystem ts, Map bounds) {
	this.ts = ts;
	this.bounds = new HashMap(bounds);
        this.defaultBound = ts.topLabel();
    }
    protected VarMap(JifTypeSystem ts, Map bounds, Label defaultBound) {
	this.ts = ts;
	this.bounds = new HashMap(bounds);
        this.defaultBound = defaultBound;
        if (defaultBound == null) {
            throw new IllegalArgumentException("defaultBound cannot be null");
        }
    }

    public VarMap copy() {
	return new VarMap(ts, bounds, defaultBound);
    }

    public Label boundOf(VarLabel v) {
	Label bound = (Label) bounds.get(v);

	if (bound == null) {
	    // The variable has no bound: assume the default label.
            // insert the default label into the map.
            bound = defaultBound;
            this.setBound(v, bound);
	}

	return bound;
    }

    public void setBound(VarLabel v, Label bound) {
	if (bound == null) {
	    throw new InternalCompilerError("Null bound label.");
	}
        bounds.put(v, bound);
    }
    
    public Label boundOf(DynamicLabel v) {
	Label bound = (Label) bounds.get(v);
        // for backward compatibility reasons, if there is no
        // mapping for a Dynamic label, we return null, rather than
        // the default bound.
	return bound;
    }

    public void setBound(DynamicLabel v, Label bound) {
	if (bound == null) {
	    throw new InternalCompilerError("Null bound label.");
	}
        bounds.put(v, bound);
    }
    
    public void setDefaultBound(Label defaultBound) {
	if (defaultBound == null) {
	    throw new InternalCompilerError("Null bound label.");
	}
        this.defaultBound = defaultBound;
    }
    
    public Label applyTo(Label c) {
        return c.bound(this, Collections.EMPTY_SET);
    }
    
    public Principal applyTo(Principal p) {
        if (p instanceof DynamicPrincipal) {
            DynamicPrincipal dp = (DynamicPrincipal) p;
            return dp.label(applyTo(dp.label()));
        }
        return p;
    }

    public Type applyTo(Type t) {
	if (ts.isLabeled(t)) {
	    Type baseType = ts.unlabel(t);
	    Label L = ts.labelOfType(t);
	    
	    return ts.labeledType(t.position(), applyTo(baseType), applyTo(L));
	}
        else if (t instanceof ArrayType) {
            ArrayType at = (ArrayType)t;
            return at.base(applyTo(at.base()));
        }
        else if (t instanceof JifSubstType) {
            JifSubstType jst = (JifSubstType)t;
            Map newMap = new HashMap();
            List args = new LinkedList();
            boolean diff = false;

            for (Iterator i = jst.entries(); i.hasNext();) {
                Map.Entry e = (Map.Entry)i.next();
                Object arg = e.getValue();
                Param p;
                if (arg instanceof Label) {
                    p = (Label)applyTo((Label)arg);
                }
                else if (arg instanceof Principal) {
                    p = (Principal)applyTo((Principal)arg);
                }
                else {
                    throw new InternalCompilerError(
                        "Unexpected type for entry: "
                            + arg.getClass().getName());
                }
                newMap.put(e.getKey(), p);

                if (p != arg) {
                    diff = true;
                }
            }
            if (diff) {
                JifTypeSystem ts = (JifTypeSystem)t.typeSystem();
                t = ts.subst(jst.base(), newMap);
                return t;
            }
        }

	return t;
    }	
    
    public boolean isEmpty() {
	return bounds.isEmpty();
    }
    
    public void print() {
        if (Solver.shouldReport(1)) {
            Solver.report(1, "======== VAR MAP ========");
            for (Iterator i = bounds.entrySet().iterator(); i.hasNext(); ){
                Map.Entry e = (Map.Entry) i.next();
                VarLabel var = (VarLabel) e.getKey();
                Label bound = (Label) e.getValue();
                String s = var.componentString() + " = " + bound.toString();
                if (var.description() != null) {
                    s += "    \t" + var.description();
                }
                Solver.report(1, s);
            }
            Solver.report(1, "Variables not in this map will receive " +
                             "default label of " + defaultBound);
            Solver.report(1, "=========================");
        }
    }

    public void dump( CodeWriter w) {
	w.write("======== VAR MAP ========");
	w.newline(0);
        for (Iterator i = bounds.entrySet().iterator(); i.hasNext(); ){
            Map.Entry e = (Map.Entry) i.next();
            VarLabel var = (VarLabel) e.getKey();
            Label bound = (Label) e.getValue();
            String s = var.componentString() + " = " + bound.toString();
            if (var.description() != null) {
                s += "    \t" + var.description();
            }
            w.write(s);
            w.newline(0);
        }
        w.write("Variables not in this map will receive " +
                         "default label of " + defaultBound);
        w.newline(0);
        w.write("=========================");
        w.newline(0);
    }
}
