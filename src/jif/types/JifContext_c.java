package jif.types;

import java.util.*;

import jif.types.hierarchy.*;
import jif.types.label.*;
import jif.types.principal.Principal;
import polyglot.ext.jl.types.Context_c;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;

/** An implementation of the <code>JifContext</code> interface.
 */
public class JifContext_c extends Context_c implements JifContext
{
    private final TypeSystem jlts;
    private final JifTypeSystem jifts;
    
    private LabelEnv env; // label environment (ph, constraints known to be true)

    private Set auth;
    private Label pc; //internal pc
    private Label entryPC; //external pc

    /**
     * Map from JifContext_c.Key (pairs of Branch.Kind and String) to Labels. 
     */
    private Map gotos;

    private boolean checkingInits;
    private Label constructorReturnLabel;

    private ReferenceType objType; // the type of the current object
    private Label objLabel;        // the label of the current object

    private LabelInstantiator labelInstantiator;
    
    JifContext_c(JifTypeSystem ts, TypeSystem jlts) {
	super(ts);
        this.jlts = jlts;
        this.jifts = ts;
        this.labelInstantiator = null;
        this.env = new LabelEnv_c();
    }

    public Object copy() {
        JifContext_c ctxt = (JifContext_c)super.copy();
        if (auth != null) {
            ctxt.auth = new HashSet(auth);
        }
        ctxt.env = env.copy();
        ctxt.labelInstantiator = null;
        return ctxt;        
    }

    public VarInstance findVariableSilent(String name) {
        VarInstance vi = super.findVariableSilent(name);

        if (vi != null) {
            return vi;
        }

        // Principals are masquerading as classes.   Find the class
        // and pull the principal out of the class.  Ick.
        ClassType principal;
        
        try {
            principal = (ClassType)jlts.typeForName("jif.lang.Principal");
        }
        catch (SemanticException e) {
            throw new InternalCompilerError("Cannot find jif.lang.Principal class.");
        }

        Named n;
        try {
            // Look for the principal only in class files.
            String className = "jif.principal." + name;
            n = jlts.loadedResolver().find(className);
        } catch (SemanticException e) {
            return null;
        }

        if (n instanceof Type) {
            Type t = (Type) n;
            if (t.isClass()) {
                Type st = t.toClass().superType();

                if (jlts.isSubtype(st, principal)) {
                    return jifts.principalInstance(null,
                        jifts.externalPrincipal(null, name));
                }
            }
        }
        return null;
    }

    public LabelEnv labelEnv() {
	return env;
    }

    public void addAssertionLE(Label L1, Label L2) {
        labelEnv().addAssertionLE(L1, L2);
    }
    
    /**
     * Adds the assertion to this context, and all outer contexts up to
     * the method/constructor/initializer level
     * @param L1
     * @param L2
     */
    public void addDefinitionalAssertionLE(Label L1, Label L2) {
        this.addAssertionLE(L1, L2);
        JifContext_c jc = this;
        do {
            jc = (JifContext_c)jc.pop();
            if (jc != null) jc.addAssertionLE(L1, L2);
        } while (jc != null && !jc.isCode());
    }

    public void addActsFor(Principal p1, Principal p2) {
	env.addActsFor(p1, p2);
    }
    public boolean actsFor(Principal p1, Principal p2) {
        return ph().actsFor(p1, p2);
    }
    public boolean actsFor(Collection actorGrp, Collection grantorGrp) {
        return ph().actsFor(actorGrp, grantorGrp);
    }


    public void clearPH() {
	env.ph().clear();
    }

    static class Key {
        polyglot.ast.Branch.Kind kind;
        String label;

        Key(polyglot.ast.Branch.Kind kind, String label) {
            this.kind = kind;
            this.label = label;
        }

        public int hashCode() {
            return kind.hashCode() + (label == null ? 0 : label.hashCode());
        }

        public boolean equalsImpl(TypeObject o) {
            return o instanceof Key
                && ((Key) o).kind.equals(kind)
                && (((Key) o).label == null
                    ? label == null
                    : ((Key) o).label.equals(label));
        }
    }

    public Label gotoLabel(polyglot.ast.Branch.Kind kind, String label) {
        if (gotos == null) return null;
        return (Label) gotos.get(new Key(kind, label));
    }

    public void gotoLabel(polyglot.ast.Branch.Kind kind, String label, Label L) {
        if (gotos == null) gotos = new HashMap();
        gotos.put(new Key(kind, label), L);
    }

    public Label entryPC() { return entryPC; }
    public void setEntryPC(Label entryPC) { this.entryPC = entryPC; }

    public Label pc() { return pc; }
    public void setPc(Label pc) { this.pc = pc; }

    public Set authority() { return auth; }
    public void setAuthority(Set auth) { this.auth = auth; }

    public PrincipalHierarchy ph() { return env.ph(); }

    public Label authLabel() {
	Set auth = authority();

	Label L = jifts.bottomLabel();

	for (Iterator i = auth.iterator(); i.hasNext(); ) {
	    Principal p = (Principal) i.next();
	    L = L.join(jifts.policyLabel(p.position(), p, Collections.EMPTY_SET));
	}

	return L;
    }

    public boolean checkingInits() {
        return checkingInits;
    }

    public void setCheckingInits(boolean checkingInits) {
        this.checkingInits = checkingInits;
    }

    public Label constructorReturnLabel() {
        return constructorReturnLabel;
    }

    public void setConstructorReturnLabel(Label Lr) {
        this.constructorReturnLabel = Lr;
    }

    public JifContext objectTypeAndLabel(ReferenceType t, Label L) {
        JifContext_c c = (JifContext_c) copy();
        c.outer = this;
        c.objType = t;
        c.objLabel = L;
        return c;
    }

    public Label instantiate(Label L, boolean instantiateThisLabels) {
        if (L == null) return L;
        try {
            if (labelInstantiator == null || labelInstantiator.ctxt != this) 
                labelInstantiator = new LabelInstantiator(this);
            labelInstantiator.setInstantiateThisLabels(instantiateThisLabels);
            return L.subst(labelInstantiator);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected SemanticException " +
            "during label substitution: " + e.getMessage(), L.position());
        }
    }

    public Principal instantiate(Principal p, boolean instantiateThisLabels) {
        if (p == null) return p;
        
        try {
            if (labelInstantiator == null || labelInstantiator.ctxt != this) 
                labelInstantiator = new LabelInstantiator(this);
            labelInstantiator.setInstantiateThisLabels(instantiateThisLabels);
            return p.subst(labelInstantiator);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected SemanticException " +
            "during label substitution: " + e.getMessage(), p.position());
        }
    }

    public Type instantiate(Type t, boolean instantiateThisLabels) throws SemanticException
    {
    if (t instanceof JifSubstType) {
        JifSubstType jit = (JifSubstType) t;
            Map newMap = new HashMap();
        boolean diff = false;
        for (Iterator i = jit.entries(); i.hasNext(); ) {
                Map.Entry e = (Map.Entry) i.next();
        Object arg = e.getValue();
        Param p;
        if (arg instanceof Label)
            p = instantiate((Label)arg, instantiateThisLabels);
        else {
            //System.out.println("### instantiate principal " + arg + " "
            //        + arg.getClass());
            p = instantiate((Principal)arg, instantiateThisLabels);
            //System.out.println("### " + p + " " + p.getClass());
        }

                newMap.put(e.getKey(), p);

        if (p != arg)
            diff = true;
        }
        if (diff) {
                return jifts.subst(jit.base(), newMap);
        }
    }
    if (t instanceof ArrayType) {
        ArrayType at = (ArrayType) t;
        Type baseType = at.base();
        return at.base(instantiate(baseType, instantiateThisLabels));
    }
    if (jifts.isLabeled(t)) {
        Label newL = instantiate(jifts.labelOfType(t), instantiateThisLabels);
        Type newT = instantiate(jifts.unlabel(t), instantiateThisLabels);
            return jifts.labeledType(newT.position(), newT, newL);
    }

    return t;
    }    
    private static class LabelInstantiator extends LabelSubstitution {
        private JifContext_c ctxt;
        private boolean instantiateThisLabels;
        protected LabelInstantiator(JifContext_c c) {
            this.ctxt = c;
        }
        
        public void setInstantiateThisLabels(boolean instantiateThisLabels) {
            this.instantiateThisLabels = instantiateThisLabels;
        }
      
        public Label substLabel(Label L) {
            Label result = L;
            if (this.instantiateThisLabels && ctxt.objLabel != null) {
                if (L instanceof CovariantThisLabel || L instanceof ThisLabel) {
                    result = ctxt.objLabel;
                }
            }

            if (ctxt.objType instanceof JifSubstType) {
                JifSubstType t = (JifSubstType)ctxt.objType;
                result = ((JifSubst)t.subst()).substLabel(result);
            }

            return result;
        }

        public Principal substPrincipal(Principal p) {
            //FIXME: should be more complex
            if (ctxt.objType != null && (ctxt.objType instanceof JifSubstType)) {
                JifSubst subst = (JifSubst) ((JifSubstType)ctxt.objType).subst();
                return subst.substPrincipal(p);
            }
            return p;
        }

    }
}
