package jif.types;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jif.JifOptions;
import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.LabelEnv_c;
import jif.types.hierarchy.PrincipalHierarchy;
import jif.types.label.AccessPath;
import jif.types.label.Label;
import jif.types.label.NotTaken;
import jif.types.label.PairLabel;
import jif.types.label.ProviderLabel;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;

import polyglot.ast.Expr;
import polyglot.ast.Local;
import polyglot.types.ClassType;
import polyglot.types.CodeInstance;
import polyglot.types.Context;
import polyglot.types.Context_c;
import polyglot.types.LocalInstance;
import polyglot.types.Named;
import polyglot.types.ParsedClassType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.VarInstance;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** An implementation of the <code>JifContext</code> interface.
 */
public class JifContext_c extends Context_c implements JifContext {
    protected final TypeSystem jlts;
    protected final JifTypeSystem jifts;

    /**
     * The label environment contains the principal hierarchy and the
     * constraints that are known to be true. In principle, this should be
     * copied when a new context is pushed, but for performance, this is copied
     * on write.
     */
    private LabelEnv_c env;

    private Set<Principal> auth;
    private Label pc; //internal pc
    private Label currentCodePCBound; //external pc

    /**
     * Map of local variables that have been endorsed
     * using a checked endorse statement
     */
    protected Map<LocalInstance, Label> checkedEndorsements;

    /**
     * Map from JifContext_c.Key (pairs of Branch.Kind and String) to Labels.
     */
    protected Map<Key, Label> gotos;

    protected boolean checkingInits;
    protected boolean inConstructorCall;
    protected boolean inPrologue = false;

    protected Label constructorReturnLabel;

    /**
     * Limit authority of classes and code in this context.
     */
    protected ProviderLabel provider;

    protected JifContext_c(JifTypeSystem ts, TypeSystem jlts) {
        super(ts);
        this.jlts = jlts;
        this.jifts = ts;
        this.env = (LabelEnv_c) ts.createLabelEnv();
    }

    @Override
    public JifContext_c copy() {
        JifContext_c ctxt = (JifContext_c) super.copy();
        if (auth != null) {
            ctxt.auth = new LinkedHashSet<Principal>(auth);
        }
        if (gotos != null) {
            ctxt.gotos = new HashMap<Key, Label>(gotos);
        }
        ctxt.provider = provider;
        return ctxt;
    }

    @Override
    public VarInstance findVariableSilent(String name) {
        VarInstance vi = super.findVariableSilent(name);

        if (vi != null) {
            return vi;
        }

        return findStaticPrincipal(name);
    }

    protected VarInstance findStaticPrincipal(String name) {
        // Principals are masquerading as classes.   Find the class
        // and pull the principal out of the class.  Ick.
        ClassType principal;

        try {
            principal =
                    (ClassType) jlts.typeForName(jifts.PrincipalClassName());
        } catch (SemanticException e) {
            throw new InternalCompilerError(
                    "Cannot find " + jifts.PrincipalClassName() + " class.", e);
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

                if (jlts.isSubtype(t.toClass(), principal)) {
                    return jifts.principalInstance(null,
                            jifts.externalPrincipal(null, name));
                }
            }
        }
        return null;
    }

    @Override
    public LabelEnv labelEnv() {
        return env;
    }

    /*
     * Called when the label environment is about to be modified.
     * This makes sure that we are dealing with a copy of the environment,
     * and do not accidentally modify the parent's environment
     */
    protected void envModification() {
        JifContext_c jifOuter = (JifContext_c) this.outer;
        if (jifOuter != null && jifOuter.env == this.env) {
            // the outer's label environment points to the same object as this
            // one. Create a new label env.
            this.env = this.env.copy();
        }
    }

    @Override
    public void addAssertionLE(Label L1, Label L2) {
        envModification();
        env.addAssertionLE(L1, L2);
    }

    @Override
    public void addActsFor(Label L, Principal p) {
        addAssertionLE(L, jifts.toLabel(p));
    }

    @Override
    public void addDefinitionalAssertionEquiv(Label L1, Label L2) {
        addDefinitionalAssertionEquiv(L1, L2, false);
    }

    /**
     * Adds the assertion to this context, and all outer contexts up to
     * the method/constructor/initializer level
     * @param L1
     * @param L2
     */
    @Override
    public void addDefinitionalAssertionEquiv(Label L1, Label L2,
            boolean addToClass) {
        // don't bother copying the environment, as we'll be
        // propogating it upwards anyway.
        // envModification();
        env.addEquiv(L1, L2);
        JifContext_c jc = this;
        LabelEnv_c lastEnvAddedTo = env;
        while (jc != null && (!jc.isCode() || addToClass)) {
            jc = (JifContext_c) jc.pop();
            if (jc != null && jc.scope == this.scope
                    && jc.env != lastEnvAddedTo) {
                // only add to env we haven't seen yet, and
                // envs in the scope of the same class as us.
                jc.env.addEquiv(L1, L2);
                lastEnvAddedTo = jc.env;
            }
        }
    }

    @Override
    public void addDefinitionalAssertionEquiv(AccessPath p, AccessPath q) {
        // don't bother copying the environment, as we'll be
        // propogating it upwards anyway.
        // envModification();
        env.addEquiv(p, q);
        JifContext_c jc = this;
        LabelEnv_c lastEnvAddedTo = env;
        while (!jc.isCode()) {
            jc = (JifContext_c) jc.pop();
            if (jc != null && jc.scope == this.scope
                    && jc.env != lastEnvAddedTo) {
                // only add to env we haven't seen yet, and
                // envs in the scope of the same class as us.
                jc.env.addEquiv(p, q);
                lastEnvAddedTo = jc.env;
            }
        }
    }

    @Override
    public void addEquiv(Label L1, Label L2) {
        envModification();
        env.addEquiv(L1, L2);
    }

    @Override
    public void addEquiv(Principal p1, Principal p2) {
        envModification();
        env.addEquiv(p1, p2);
    }

    @Override
    public void addActsFor(Principal p1, Principal p2) {
        envModification();
        env.addActsFor(p1, p2);
    }

    @Override
    public void addActsFor(ActsForParam actor, Principal granter) {
        if (actor instanceof Label) {
            addActsFor((Label) actor, granter);
        } else if (actor instanceof Principal) {
            addActsFor((Principal) actor, granter);
        } else {
            throw new InternalCompilerError(
                    "Unexpected ActsForParam type: " + actor.getClass());
        }
    }

    @Override
    public void addEquiv(AccessPath p, AccessPath q) {
        //envModification(); XXX add the equivalence to the current environment.
//        env.addEquiv(p, q);
        // don't bother copying the environment, as we'll be
        // propogating it upwards anyway.
        // envModification();
        env.addEquiv(p, q);
        JifContext_c jc = this;
        LabelEnv_c lastEnvAddedTo = env;
        boolean lastJCBlock = (jc.kind == Context_c.BLOCK);
        while (!jc.isCode() && !lastJCBlock) {
            jc = (JifContext_c) jc.pop();
            if (jc != null && jc.scope == this.scope
                    && jc.env != lastEnvAddedTo) {
                // only add to env we haven't seen yet, and
                // envs in the scope of the same class as us.
                jc.env.addEquiv(p, q);
                lastEnvAddedTo = jc.env;
            }
            lastJCBlock = (jc.kind == Context_c.BLOCK);
        }

    }

    /**
     * Adds the assertion to this context, and all outer contexts up to
     * the method/constructor/initializer level
     */
    @Override
    public void addDefinitionalEquiv(Principal p1, Principal p2) {
        // don't bother copying the environment, as we'll be
        // propogating it upwards anyway.
        // envModification();
        this.addEquiv(p1, p2);
        JifContext_c jc = this;
        LabelEnv_c lastEnvAddedTo = env;
        while (!jc.isCode()) {
            jc = (JifContext_c) jc.pop();
            if (jc != null && jc.scope == this.scope
                    && jc.env != lastEnvAddedTo) {
                // only add to env we haven't seen yet, and
                // envs in the scope of the same class as us.
                jc.env.addEquiv(p1, p2);
                lastEnvAddedTo = jc.env;
            }
        }
    }

    @Override
    public void clearPH() {
        envModification();
        env.ph().clear();
    }

    protected static class Key {
        protected polyglot.ast.Branch.Kind kind;
        protected String label;

        public Key(polyglot.ast.Branch.Kind kind, String label) {
            this.kind = kind;
            this.label = label;
        }

        @Override
        public int hashCode() {
            return kind.hashCode() + (label == null ? 0 : label.hashCode());
        }

        @Override
        public String toString() {
            return kind.toString() + label;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Key) {
                Key that = (Key) o;
                return this.kind.equals(that.kind)
                        && (this.label == that.label || (this.label != null
                                && this.label.equals(that.label)));
            }
            return false;
        }
    }

    @Override
    public Label gotoLabel(polyglot.ast.Branch.Kind kind, String label) {
        if (gotos == null) return null;
        return gotos.get(new Key(kind, label));
    }

    @Override
    public void gotoLabel(polyglot.ast.Branch.Kind kind, String label,
            Label L) {
        if (gotos == null) gotos = new HashMap<Key, Label>();
        gotos.put(new Key(kind, label), L);
    }

    @Override
    public Label currentCodePCBound() {
        return currentCodePCBound;
    }

    @Override
    public void setCurrentCodePCBound(Label currentCodePCBound) {
        this.currentCodePCBound = currentCodePCBound;
    }

    @Override
    public Label pc() {
        return pc;
    }

    @Override
    // pc label is special for better error report
    public void setPc(Label pc, LabelChecker lc) { // this.pc = pc;
        if (pc instanceof NotTaken) this.pc = pc;
        if (this.pc == pc) return;
        if (pc != null) {
            Set<Label> set = new HashSet<Label>();
            set.add(pc);
            this.pc = lc.jifTypeSystem().joinLabel(pc.position(), set);
            this.pc.setDescription("pc label");
        } else this.pc = pc;
    }

    @Override
    public Set<Principal> authority() {
        return auth;
    }

    @Override
    public void setAuthority(Set<Principal> auth) {
        this.auth = auth;
    }

    @Override
    public PrincipalHierarchy ph() {
        return env.ph();
    }

    @Override
    public Label authLabel() {
        JifOptions opt = (JifOptions) jifts.extensionInfo().getOptions();
        if (opt.authFromProvider()) {
            Position pos = Position.compilerGenerated();
            PairLabel public_untrusted = jifts.pairLabel(pos,
                    jifts.bottomConfPolicy(pos), jifts.topIntegPolicy(pos));
            Label provider_integ = jifts.meet(public_untrusted, provider());
            Label L = jifts.writersToReadersLabel(pos, provider_integ);
            return env.triggerTransforms(L).normalize();
        } else {
            Set<Principal> auth = authority();

            Set<Label> labels = new LinkedHashSet<Label>();
            for (Principal p : auth) {
                PairLabel pl = jifts.pairLabel(p.position(),
                        jifts.readerPolicy(p.position(), p,
                                jifts.topPrincipal(p.position())),
                        jifts.topIntegPolicy(p.position()));
                labels.add(pl);
            }

            if (labels.isEmpty()) {
                return jifts.bottomLabel(currentCode().position());
            }
            Label L = jifts.joinLabel(currentCode().position(), labels);
            return L;
        }
    }

    @Override
    public Label authLabelInteg() {
        JifOptions opt = (JifOptions) jifts.extensionInfo().getOptions();
        if (opt.authFromProvider()) {
            Position pos = Position.compilerGenerated();
            return jifts.pairLabel(pos, jifts.bottomConfPolicy(pos),
                    provider().integProjection());
        } else {
            Set<Label> labels = new LinkedHashSet<Label>();
            for (Principal p : authority()) {
                PairLabel pl = jifts.pairLabel(p.position(),
                        jifts.bottomConfPolicy(p.position()),
                        jifts.writerPolicy(p.position(), p,
                                jifts.topPrincipal(p.position())));
                labels.add(pl);
            }

            if (labels.isEmpty()) {
                return jifts.topLabel(currentCode().position());
            }
            Label L = jifts.meetLabel(currentCode().position(), labels);
            return L;
        }
    }

    @Override
    public boolean checkingInits() {
        return checkingInits;
    }

    @Override
    public void setCheckingInits(boolean checkingInits) {
        this.checkingInits = checkingInits;
    }

    @Override
    public Label constructorReturnLabel() {
        return constructorReturnLabel;
    }

    @Override
    public void setConstructorReturnLabel(Label Lr) {
        this.constructorReturnLabel = Lr;
    }

    @Override
    public Context pushConstructorCall() {
        JifContext_c A = (JifContext_c) pushStatic();
        A.inConstructorCall = true;
        return A;
    }

    @Override
    public Context pushClass(ParsedClassType classScope, ClassType type) {
        JifContext_c jc = (JifContext_c) super.pushClass(classScope, type);
        // force a new label environment
        jc.envModification();
        return jc;
    }

    @Override
    public Context pushCode(CodeInstance ci) {
        JifContext_c jc = (JifContext_c) super.pushCode(ci);
        // force a new label environment
        jc.envModification();
        return jc;
    }

//    public Context pushBlock() {
//        JifContext_c jc = (JifContext_c)super.pushBlock();
//        // force a new label environment
//        jc.envModification();
//        return jc;
//    }

    @Override
    public boolean inConstructorCall() {
        return this.inConstructorCall;
    }

    @Override
    public PathMap pathMapForLocal(LocalInstance li, LabelChecker lc) {
        JifTypeSystem ts = lc.jifTypeSystem();
        Label L = null;
        if (checkedEndorsements != null
                && checkedEndorsements.containsKey(li)) {
            L = checkedEndorsements.get(li);
        } else {
            L = ts.labelOfLocal(li, this.pc());
        }

        PathMap X = ts.pathMap();
        X = X.N(this.pc());
        X = X.NV(lc.upperBound(L, this.pc()));
        return X;
    }

    @Override
    public boolean updateAllowed(Expr e) {
        if (e instanceof Local && checkedEndorsements != null) {
            // cannot update locals that are involved in a checked endorse
            return !checkedEndorsements
                    .containsKey(((Local) e).localInstance());
        }
        return true;
    }

    @Override
    public void addCheckedEndorse(LocalInstance li, Label downgradeTo) {
        if (this.checkedEndorsements == null) {
            this.checkedEndorsements = new HashMap<LocalInstance, Label>();
        } else {
            this.checkedEndorsements =
                    new HashMap<LocalInstance, Label>(this.checkedEndorsements);
        }
        this.checkedEndorsements.put(li, downgradeTo);
    }

    @Override
    public ProviderLabel provider() {
        return provider;
    }

    @Override
    public void setProvider(ProviderLabel provider) {
        this.provider = provider;
    }

    @Override
    public Context pushPrologue() {
        JifContext_c v = this.copy();
        v.outer = this;
        /** Prologue does not have separate lexical scope: inherit types and vars:  */
        v.kind = BLOCK;
        v.inPrologue = true;
        return v;
    }

    public boolean inPrologue() {
        return inPrologue;
    }
}
