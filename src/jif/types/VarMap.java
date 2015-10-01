package jif.types;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import jif.types.label.Label;
import jif.types.label.Policy;
import jif.types.label.VarLabel;
import jif.types.label.Variable;
import jif.types.principal.Principal;
import jif.types.principal.VarPrincipal;
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
    protected Map<Variable, ActsForParam> bounds;
    protected JifTypeSystem ts;
    protected final Label defaultLabelBound;
    protected final Principal defaultPrincipalBound;

    public VarMap(JifTypeSystem ts, Label defaultLabelBound,
            Principal defaultPrincipalBound) {
        this.ts = ts;
        this.bounds = new LinkedHashMap<Variable, ActsForParam>();
        this.defaultLabelBound = defaultLabelBound;
        this.defaultPrincipalBound = defaultPrincipalBound;
        if (defaultLabelBound == null || defaultPrincipalBound == null) {
            throw new InternalCompilerError("default bounds cannot be null");
        }
    }

    private VarMap(JifTypeSystem ts, Map<Variable, ActsForParam> bounds,
            Label defaultLabelBound, Principal defaultPrincipalBound) {
        this.ts = ts;
        this.bounds = new LinkedHashMap<Variable, ActsForParam>(bounds);
        this.defaultLabelBound = defaultLabelBound;
        this.defaultPrincipalBound = defaultPrincipalBound;
        if (defaultLabelBound == null || defaultPrincipalBound == null) {
            throw new InternalCompilerError("default bounds cannot be null");
        }
    }

    public VarMap copy() {
        return new VarMap(ts, bounds, defaultLabelBound, defaultPrincipalBound);
    }

    public Principal boundOf(VarPrincipal v) {
        Principal bound = (Principal) bounds.get(v);

        if (bound == null) {
            // The variable has no bound: assume the default bound.
            // insert the default label into the map.
            bound = defaultPrincipalBound;
            this.setBound(v, bound);
        }

        return bound;
    }

    public Label boundOf(VarLabel v) {
        Label bound = (Label) bounds.get(v);

        if (bound == null) {
            // The variable has no bound: assume the default label.
            // insert the default label into the map.
            bound = defaultLabelBound;
            this.setBound(v, bound);
        }

        return bound;
    }

    public void setBound(Variable v, Label bound) {
        if (bound == null) {
            throw new InternalCompilerError("Null bound label.");
        }
        bounds.put(v, bound);
    }

    public void setBound(Variable v, Principal bound) {
        if (bound == null) {
            throw new InternalCompilerError("Null bound principal.");
        }
        bounds.put(v, bound);
    }

    private class VarMapLabelSubstitution extends LabelSubstitution {
        @Override
        public Label substLabel(Label L) throws SemanticException {
            if (L instanceof VarLabel) {
                VarLabel v = (VarLabel) L;
                return VarMap.this.boundOf(v);
            }
            return L;
        }

        @Override
        public Principal substPrincipal(Principal p) throws SemanticException {
            if (p instanceof VarPrincipal) {
                VarPrincipal v = (VarPrincipal) p;
                return VarMap.this.boundOf(v);
            }
            return p;
        }
    }

    public Policy applyTo(Policy p) {
        LabelSubstitution s = new VarMapLabelSubstitution();
        try {
            return p.subst(s);
        } catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected SemanticException", e);
        }

    }

    public Param applyTo(Param c) {
        if (c instanceof Label) return applyTo((Label) c);
        if (c instanceof Principal) return applyTo((Principal) c);
        throw new InternalCompilerError("Unexpected Param" + c);
    }

    public Label applyTo(Label c) {
        LabelSubstitution s = new VarMapLabelSubstitution();
        try {
            return c.subst(s);
        } catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected SemanticException", e);
        }
    }

    public void applyTo(Equation eqn) {
        // TODO: this imperatively updates eqn.
        LabelSubstitution s = new VarMapLabelSubstitution();
        try {
            eqn.subst(s);
        } catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected SemanticException", e);
        }
    }

    public Principal applyTo(Principal p) {
        LabelSubstitution s = new VarMapLabelSubstitution();
        try {
            return p.subst(s);
        } catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected SemanticException", e);
        }
    }

    public Type applyTo(Type t) {
        if (ts.isLabeled(t)) {
            Type baseType = ts.unlabel(t);
            Label L = ts.labelOfType(t);

            return ts.labeledType(t.position(), applyTo(baseType), applyTo(L));
        } else if (t instanceof ArrayType) {
            ArrayType at = (ArrayType) t;
            return at.base(applyTo(at.base()));
        } else if (t instanceof JifSubstType) {
            JifSubstType jst = (JifSubstType) t;
            Map<ParamInstance, Param> newMap =
                    new LinkedHashMap<ParamInstance, Param>();
            boolean diff = false;

            for (Iterator<Entry<ParamInstance, Param>> i = jst.entries(); i
                    .hasNext();) {
                Entry<ParamInstance, Param> e = i.next();
                Param arg = e.getValue();
                Param p;
                if (arg instanceof Label) {
                    p = applyTo((Label) arg);
                } else if (arg instanceof Principal) {
                    p = applyTo((Principal) arg);
                } else {
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
                JifTypeSystem ts = (JifTypeSystem) t.typeSystem();
                t = ts.subst(jst.base(), newMap);
                return t;
            }
        }

        return t;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("======== VAR MAP ========");
        sb.append('\n');
        for (Entry<Variable, ActsForParam> e : bounds.entrySet()) {
            Variable var = e.getKey();
            ActsForParam bound = e.getValue();
            String s = "";
            if (var instanceof VarLabel) {
                s = ((VarLabel) var).componentString() + " = "
                        + bound.toString();
                if (((VarLabel) var).description() != null) {
                    s += "    \t" + ((VarLabel) var).description();
                }
            } else {
                s = var + " = " + bound.toString();
            }
            sb.append(s);
            sb.append('\n');
        }
        sb.append("Variables not in this map will receive "
                + "default bound of " + defaultLabelBound + " or "
                + defaultPrincipalBound + " as appropriate.");
        sb.append('\n');
        sb.append("=========================\n");
        return sb.toString();
    }

    public void dump(CodeWriter w) {
        w.write("======== VAR MAP ========");
        w.newline(0);
        for (Entry<Variable, ActsForParam> e : bounds.entrySet()) {
            Variable var = e.getKey();
            ActsForParam bound = e.getValue();
            String s = "";
            if (var instanceof VarLabel) {
                s = ((VarLabel) var).componentString() + " = "
                        + bound.toString();
                if (((VarLabel) var).description() != null) {
                    s += "    \t" + ((VarLabel) var).description();
                }
            } else {
                s = var + " = " + bound.toString();
            }
            w.write(s);
            w.newline(0);
        }
        w.write("Variables not in this map will receive " + "default bound of "
                + defaultLabelBound + " or " + defaultPrincipalBound
                + " as appropriate.");
        w.newline(0);
        w.write("=========================");
        w.newline(0);
    }
}
