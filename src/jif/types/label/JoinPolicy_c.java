package jif.types.label;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import jif.types.PathMap;
import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.LabelEnv.SearchState;
import jif.visit.LabelChecker;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** Represents a join of a number of policies. 
 */
public abstract class JoinPolicy_c<P extends Policy> extends Policy_c
        implements JoinPolicy<P> {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private final Set<P> joinComponents;
    private Integer hashCode = null;

    public JoinPolicy_c(Set<P> components, JifTypeSystem ts, Position pos) {
        super(ts, pos);
        this.joinComponents = Collections.unmodifiableSet(flatten(components));
        if (this.joinComponents.isEmpty()) {
            throw new InternalCompilerError("Empty collection!");
        }
    }

    @Override
    public boolean isSingleton() {
        return joinComponents.size() == 1;
    }

    @Override
    public boolean isCanonical() {
        for (P c : joinComponents) {
            if (!c.isCanonical()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        for (P c : joinComponents) {
            if (!c.isRuntimeRepresentable()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (o instanceof JoinPolicy_c) {
            @SuppressWarnings("rawtypes")
            JoinPolicy_c that = (JoinPolicy_c) o;
            return this.hashCode() == that.hashCode()
                    && this.joinComponents.equals(that.joinComponents);
        }
        if (o instanceof Policy) {
            // see if it matches a singleton
            return this.joinComponents.equals(Collections.singleton(o));
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (hashCode == null) {
            hashCode = new Integer(joinComponents.hashCode());
        }
        return hashCode.intValue();
    }

    @Override
    public String toString(Set<Label> printedLabels) {
        String s = "";
        for (Iterator<P> i = joinComponents.iterator(); i.hasNext();) {
            P c = i.next();
            s += c.toString(printedLabels);

            if (i.hasNext()) {
                s += "; ";
            }
        }

        return s;
    }

    protected boolean leq_(Policy p, LabelEnv env, SearchState state) {
        // If this = { .. Pi .. } and L = { .. Pj' .. }, check if for all i,
        // there exists a j, such that Pi <= Pj'
        for (P pi : joinComponents) {
            if (!env.leq(pi, p, state)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Collection<P> joinComponents() {
        return Collections.unmodifiableCollection(joinComponents);
    }

    /**
     * @return An equivalent label with fewer components by pulling out
     * less restrictive policies.
     */
    @Override
    protected Policy simplifyImpl() {
        if (joinComponents.isEmpty()) {
            return this;
        }
        if (joinComponents.size() == 1) {
            return joinComponents.iterator().next().simplify();
        }

        Collection<P> comps = flatten(joinComponents);
        Set<P> needed = new LinkedHashSet<P>();
        JifTypeSystem jts = (JifTypeSystem) ts;

        for (final P comp : comps) {
            @SuppressWarnings("unchecked")
            P ci = (P) comp.simplify();

            boolean subsumed = false;

            if (ci.hasVariables() || ci.hasWritersToReaders()) {
                needed.add(ci);
            } else {
                for (Iterator<P> j = needed.iterator(); j.hasNext();) {
                    P cj = j.next();

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

        if (needed.equals(joinComponents)) {
            return this;
        }
        if (needed.size() == 1) {
            return needed.iterator().next();
        }

        return constructJoinPolicy(needed, position);
    }

    protected abstract Policy constructJoinPolicy(Set<P> components,
            Position pos);

    private static <P extends Policy> Set<P> flatten(Set<P> comps) {
        // check if there are any join policies in there.
        boolean needFlattening = false;
        for (P p : comps) {
            if (p instanceof JoinPolicy_c) {
                needFlattening = true;
                break;
            }
        }

        if (!needFlattening) return comps;

        Set<P> c = new LinkedHashSet<P>();
        for (P p : comps) {
            if (p.isTop()) {
                return Collections.singleton(p);
            }

            if (p instanceof JoinPolicy_c) {
                @SuppressWarnings("unchecked")
                JoinPolicy_c<P> jp = (JoinPolicy_c<P>) p;
                Collection<P> lComps = jp.joinComponents();
                c.addAll(lComps);
            } else {
                c.add(p);
            }
        }

        return c;
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        List<Type> throwTypes = new ArrayList<Type>();
        for (P L : joinComponents) {
            throwTypes.addAll(L.throwTypes(ts));
        }
        return throwTypes;
    }

    @Override
    public Policy subst(LabelSubstitution substitution)
            throws SemanticException {
        if (joinComponents.isEmpty()) {
            return substitution.substPolicy(this).simplify();
        }
        boolean changed = false;
        Set<P> s = new LinkedHashSet<P>();

        for (P c : joinComponents) {
            @SuppressWarnings("unchecked")
            P newc = (P) c.subst(substitution);
            if (newc != c) changed = true;
            s.add(newc);
        }

        if (!changed) return substitution.substPolicy(this).simplify();

        Policy newJoinPolicy = constructJoinPolicy(flatten(s), position);
        return substitution.substPolicy(newJoinPolicy).simplify();
    }

    @Override
    public boolean hasWritersToReaders() {
        for (P c : joinComponents) {
            if (c.hasWritersToReaders()) return true;
        }
        return false;
    }

    @Override
    public boolean hasVariables() {
        for (P c : joinComponents) {
            if (c.hasVariables()) return true;
        }
        return false;
    }

    @Override
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        JifTypeSystem ts = (JifTypeSystem) A.typeSystem();
        PathMap X = ts.pathMap().N(A.pc()).NV(A.pc());

        if (joinComponents.isEmpty()) {
            return X;
        }

        A = (JifContext) A.pushBlock();

        for (P c : joinComponents) {
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

    @Override
    public boolean isTop() {
        // top if any policy is top
        for (P c : joinComponents) {
            if (c.isTop()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBottom() {
        // bottom if all policies are bottom
        for (P c : joinComponents) {
            if (!c.isBottom()) {
                return false;
            }
        }
        return true;
    }
}
