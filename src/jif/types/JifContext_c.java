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
    private boolean inConstructorCall;
    private Label constructorReturnLabel;
    
    JifContext_c(JifTypeSystem ts, TypeSystem jlts) {
	super(ts);
        this.jlts = jlts;
        this.jifts = ts;
        this.env = new LabelEnv_c(ts);
    }

    public Object copy() {
        JifContext_c ctxt = (JifContext_c)super.copy();
        if (auth != null) {
            ctxt.auth = new HashSet(auth);
        }
        ctxt.env = env.copy();
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
            throw new InternalCompilerError("Cannot find jif.lang.Principal class.", e);
        }

        Named n;
        try {
            // Look for the principal only in class files.
            String className = "jif.principals." + name;
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
    public void addDefinitionalAssertionEquiv(Label L1, Label L2) {
        this.addAssertionLE(L1, L2);
        this.addAssertionLE(L2, L1);
        JifContext_c jc = this;
        while (!jc.isCode()) {
            jc = (JifContext_c)jc.pop();
            if (jc != null) {
                jc.addAssertionLE(L1, L2);
                jc.addAssertionLE(L2, L1);
            }            
        }
    }
    
    public void addEquiv(Principal p1, Principal p2) {
        env.addEquiv(p1, p2);
    }
    public void addActsFor(Principal p1, Principal p2) {
        env.addActsFor(p1, p2);
    }
    /**
     * Adds the assertion to this context, and all outer contexts up to
     * the method/constructor/initializer level
     */
    public void addDefinitionalEquiv(Principal p1, Principal p2) {
        this.addEquiv(p1, p2);
        JifContext_c jc = this;
        while (!jc.isCode()) {
            jc = (JifContext_c)jc.pop();
            if (jc != null) {
                jc.addEquiv(p1, p2);
            }            
        }
    }
    public boolean actsFor(Principal p1, Principal p2) {
        return ph().actsFor(p1, p2);
    }
    public boolean equiv(Principal p1, Principal p2) {
        return actsFor(p1, p2) && actsFor(p2, p1);
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

    public Context pushConstructorCall() {
        JifContext_c A = (JifContext_c)pushStatic();
        A.inConstructorCall = true;
        return A;
    }
    
    public boolean inConstructorCall() {
        return this.inConstructorCall;
    }
}
