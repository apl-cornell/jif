package jif.types.label;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jif.translate.LabelToJavaExpr;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import jif.types.PathMap;
import jif.types.hierarchy.LabelEnv;
import jif.visit.LabelChecker;

import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>JoinLabel</code> interface.
 */
public class JoinLabel_c extends Label_c implements JoinLabel {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private final Set<Label> components;

    public JoinLabel_c(Set<Label> components, JifTypeSystem ts, Position pos,
            LabelToJavaExpr trans) {
        super(ts, pos, trans);
        this.components = Collections.unmodifiableSet(flatten(components));
        if (this.components.isEmpty())
            throw new InternalCompilerError("No empty joins");
    }

    @Override
    public boolean isRuntimeRepresentable() {
        for (Label c : components) {
            if (!c.isRuntimeRepresentable()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isCanonical() {
        for (Label c : components) {
            if (!c.isCanonical()) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected boolean isDisambiguatedImpl() {
        return true;
    }

    /**
     * @return true iff this label is covariant.
     *
     * A label is covariant if it contains at least one covariant component.
     */
    @Override
    public boolean isCovariant() {
        for (Label c : components) {
            if (c.isCovariant()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isComparable() {
        for (Label c : components) {
            if (!c.isComparable()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isEnumerable() {
        return true;
    }

    @Override
    public boolean isBottom() {
        return (components.isEmpty());
    }

    @Override
    public boolean isTop() {
        if (components.isEmpty()) return false;
        for (Label c : components) {
            if (c.isTop()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (o instanceof JoinLabel_c) {
            JoinLabel_c that = (JoinLabel_c) o;
            return this.components.equals(that.components);
        }
        if (o instanceof Label) {
            // see if it matches a singleton
            return this.components.equals(Collections.singleton(o));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return components.hashCode();
    }

    @Override
    public String toString() {
        if (isTop()) return "{⊤→}";
        return super.toString();
    }

    @Override
    public String componentString(Set<Label> printedLabels) {
        String s = "";
        for (Iterator<Label> i = components.iterator(); i.hasNext();) {
            Label c = i.next();
            s += c.componentString(printedLabels);

            if (i.hasNext()) {
                s += " ⊔ ";
            }
        }

        return s;
    }

    @Override
    public boolean leq_(Label L, LabelEnv env, LabelEnv.SearchState state) {
        if (!L.isComparable() || !L.isEnumerable())
            throw new InternalCompilerError("Cannot compare " + L);

        // If this = {c1 join ... join cn } , check if for all i,
        // we have Pi <= L
        for (Label ci : components) {
            if (!env.leq(ci, L, state)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Collection<Label> joinComponents() {
        return Collections.unmodifiableCollection(components);
    }

    @Override
    public JoinLabel_c copy() {
        JoinLabel_c l = (JoinLabel_c) super.copy();
        l.normalized = null;
        return l;
    }

    private Label normalized = null;

    @Override
    public Label normalize() {
        if (normalized == null) {
            // memoize the result
            normalized = normalizeImpl();
        }
        return normalized;
    }

    private Label normalizeImpl() {

        if (components.size() == 1) {
            return components.iterator().next();
        }
        // if there is more than one PairLabel, combine them.
        JifTypeSystem ts = typeSystem();
        PairLabel pl = null;
        boolean combinedPL = false;
        for (Label lbl : joinComponents()) {
            if (lbl instanceof PairLabel) {
                PairLabel p = (PairLabel) lbl;
                if (pl == null) {
                    pl = p;
                } else {
                    combinedPL = true;
                    pl = ts.pairLabel(position(),
                            pl.confPolicy().join(p.confPolicy()),
                            pl.integPolicy().join(p.integPolicy()));
                }
            }
        }
        if (combinedPL) {
            Set<Label> comps = new LinkedHashSet<Label>();
            comps.add(pl);
            for (Label lbl : joinComponents()) {
                if (!(lbl instanceof PairLabel)) {
                    comps.add(lbl);
                }
            }

            return ts.joinLabel(position(), comps);
        }
        return this;
    }

    /**
     * @return An equivalent label with fewer components by pulling out
     * less restrictive policies.
     */
    @Override
    protected Label simplifyImpl() {
        if (!this.isDisambiguated() || components.isEmpty()) {
            return this;
        }

        Set<Label> needed = new LinkedHashSet<Label>();
        JifTypeSystem jts = (JifTypeSystem) ts;

        for (Label ci : components) {
            ci = ci.simplify();

            if (ci.hasVariables() || ci.hasWritersToReaders()) {
                needed.add(ci);
            } else {
                boolean subsumed = false;

                for (Iterator<Label> j = needed.iterator(); j.hasNext();) {
                    Label cj = j.next();

                    if (cj.hasVariables() || cj.hasWritersToReaders()) {
                        continue;
                    }

                    if (jts.leq(ci, cj)) {
                        subsumed = true;
                        break;
                    }

                    if (jts.leq(cj, ci)) {
                        j.remove();
                    }
                }

                if (!subsumed) needed.add(ci);
            }
        }

        if (needed.equals(components)) {
            return this;
        }
        if (needed.size() == 1) {
            return needed.iterator().next();
        }

        return jts.joinLabel(position(), needed);
    }

    private static Set<Label> flatten(Set<Label> comps) {

        // check if there are any join labels in there.
        boolean needFlattening = false;
        for (Label L : comps) {
            if (L instanceof JoinLabel) {
                needFlattening = true;
                break;
            }
        }

        if (!needFlattening) return comps;

        Set<Label> c = new LinkedHashSet<Label>();
        for (Label L : comps) {
            if (L.isTop()) {
                return Collections.singleton(L);
            }

            if (L instanceof JoinLabel) {
                Collection<Label> lComps = ((JoinLabel) L).joinComponents();
                c.addAll(lComps);
            } else {
                c.add(L);
            }
        }

        return c;
    }

    @Override
    public ConfPolicy confProjection() {
        Set<ConfPolicy> confPols = new HashSet<ConfPolicy>();
        for (Label c : components) {
            confPols.add(c.confProjection());
        }
        return ((JifTypeSystem) ts).joinConfPolicy(position, confPols);
    }

    @Override
    public IntegPolicy integProjection() {
        Set<IntegPolicy> integPols = new HashSet<IntegPolicy>();
        for (Label c : components) {
            integPols.add(c.integProjection());
        }
        return ((JifTypeSystem) ts).joinIntegPolicy(position, integPols);
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        List<Type> throwTypes = new ArrayList<Type>();
        for (Label L : components) {
            throwTypes.addAll(L.throwTypes(ts));
        }
        return throwTypes;
    }

    @Override
    public Label subst(LabelSubstitution substitution)
            throws SemanticException {
        if (components.isEmpty() || substitution.stackContains(this)
                || !substitution.recurseIntoChildren(this)) {
            return substitution.substLabel(this);
        }
        substitution.pushLabel(this);
        boolean changed = false;
        Set<Label> s = null;

        for (Label c : components) {
            Label newc = c.subst(substitution);
            if (!changed && newc != c) {
                changed = true;
                s = new LinkedHashSet<Label>();
                // add all the previous labels
                for (Label d : components) {
                    if (c == d) break;
                    s.add(d);
                }
            }
            if (changed) s.add(newc);
        }

        substitution.popLabel(this);

        if (!changed) return substitution.substLabel(this);

        JifTypeSystem ts = this.typeSystem();
        Label newJoinLabel = ts.joinLabel(this.position(), flatten(s));
        return substitution.substLabel(newJoinLabel);
    }

    @Override
    public Set<Variable> variableComponents() {
        Set<Variable> s = new LinkedHashSet<Variable>();
        for (Label ci : joinComponents()) {
            s.addAll(ci.variableComponents());
        }
        return s;
    }

    @Override
    public boolean hasWritersToReaders() {
        for (Label ci : joinComponents()) {
            if (ci.hasWritersToReaders()) return true;
        }
        return false;
    }

    @Override
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        JifTypeSystem ts = (JifTypeSystem) A.typeSystem();
        PathMap X = ts.pathMap().N(A.pc()).NV(A.pc());

        if (components.isEmpty()) {
            return X;
        }

        A = (JifContext) A.pushBlock();

        for (Label c : components) {
            updateContextForComp(lc, A, X);
            PathMap Xc = c.labelCheck(A, lc);
            X = X.join(Xc);
        }
        return X;
    }

    /**
     * Utility method for updating the context for checking a join component.
     *
     * Useful for overriding in projects like Fabric.
     */
    protected void updateContextForComp(LabelChecker lc, JifContext A,
            PathMap Xprev) {
        A.setPc(Xprev.N(), lc);
    }
}
