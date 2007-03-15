package jif.types;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import jif.types.label.Label;
import jif.types.label.Policy;
import jif.types.label.VarLabel;
import jif.types.principal.Principal;
import polyglot.types.ArrayType;
import polyglot.types.SemanticException;
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
    private final Label defaultBound;

    public VarMap(JifTypeSystem ts, Label defaultBound) {
	this.ts = ts;
	this.bounds = new LinkedHashMap();
        this.defaultBound = defaultBound;
        if (defaultBound == null) {
            throw new InternalCompilerError("defaultBound cannot be null");
        }
    }

    private VarMap(JifTypeSystem ts, Map bounds, Label defaultBound) {
	this.ts = ts;
	this.bounds = new LinkedHashMap(bounds);
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
    
    private class VarMapLabelSubstitution extends LabelSubstitution {
        public Label substLabel(Label L) throws SemanticException {
            if (L instanceof VarLabel) {
                VarLabel v = (VarLabel)L;
                return VarMap.this.boundOf(v);
            }
            return L;
        }            
    }
    
    public Policy applyTo(Policy p) {
        LabelSubstitution s = new VarMapLabelSubstitution() ;
        try {
            return p.subst(s);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected SemanticException", e);
        }
        
    }
    public Label applyTo(Label c) {
        LabelSubstitution s = new VarMapLabelSubstitution() ;
        try {
            return c.subst(s);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected SemanticException", e);
        }
    }
    
    public Principal applyTo(Principal p) {
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
            Map newMap = new LinkedHashMap();
            boolean diff = false;

            for (Iterator i = jst.entries(); i.hasNext();) {
                Map.Entry e = (Map.Entry)i.next();
                Object arg = e.getValue();
                Param p;
                if (arg instanceof Label) {
                    p = applyTo((Label)arg);
                }
                else if (arg instanceof Principal) {
                    p = applyTo((Principal)arg);
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
    
    public void print() {
        if (Solver.shouldReport(2)) {
            Solver.report(2, "======== VAR MAP ========");
            for (Iterator i = bounds.entrySet().iterator(); i.hasNext(); ){
                Map.Entry e = (Map.Entry) i.next();
                VarLabel var = (VarLabel) e.getKey();
                Label bound = (Label) e.getValue();
                String s = var.componentString() + " = " + bound.toString();
                if (var.description() != null) {
                    s += "    \t" + var.description();
                }
                Solver.report(2, s);
            }
            Solver.report(2, "Variables not in this map will receive " +
                             "default label of " + defaultBound);
            Solver.report(2, "=========================");
        }
    }

    public void dump(CodeWriter w) {
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
