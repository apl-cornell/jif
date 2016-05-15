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

/** Represents the meet of a number of policies. 
 */
public abstract class MeetPolicy_c<P extends Policy> extends Policy_c
        implements MeetPolicy<P> {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private final Set<P> meetComponents;
    private Integer hashCode = null;

    public MeetPolicy_c(Set<P> components, JifTypeSystem ts, Position pos) {
        super(ts, pos);
        this.meetComponents = Collections.unmodifiableSet(flatten(components));
        if (this.meetComponents.isEmpty()) {
            throw new InternalCompilerError("Empty collection!");
        }
    }

    @Override
    public boolean isSingleton() {
        return meetComponents.size() == 1;
    }

    @Override
    public boolean isCanonical() {
        for (P c : meetComponents) {
            if (!c.isCanonical()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        for (P c : meetComponents) {
            if (!c.isRuntimeRepresentable()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (o instanceof MeetPolicy_c) {
            @SuppressWarnings("rawtypes")
            MeetPolicy_c that = (MeetPolicy_c) o;
            return this.hashCode() == that.hashCode()
                    && this.meetComponents.equals(that.meetComponents);
        }
        if (o instanceof Policy) {
            // see if it matches a singleton
            return this.meetComponents.equals(Collections.singleton(o));
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (hashCode == null) {
            hashCode = new Integer(meetComponents.hashCode());
        }
        return hashCode.intValue();
    }

    @Override
    public String toString(Set<Label> printedLabels) {
        String s = "";
        for (Iterator<P> i = meetComponents.iterator(); i.hasNext();) {
            P c = i.next();
            s += c.toString(printedLabels);

            if (i.hasNext()) {
                s += " meet ";
            }
        }

        return s;
    }

    protected boolean leq_(Policy p, LabelEnv env, SearchState state) {
        // If this = { .. Pi .. , check there exists an i
        // such that Pi <= p
        for (P pi : meetComponents) {
            if (env.leq(pi, p, state)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Collection<P> meetComponents() {
        return Collections.unmodifiableCollection(meetComponents);
    }

    /**
     * @return An equivalent label with fewer components by pulling out
     * less restrictive policies.
     */
    @Override
    protected Policy simplifyImpl() {
        if (meetComponents.isEmpty()) {
            return this;
        }
        if (meetComponents.size() == 1) {
            return ((Policy) meetComponents.iterator().next()).simplify();
        }

        Collection<P> comps = flatten(meetComponents);
        Set<P> needed = new LinkedHashSet<P>();
        JifTypeSystem jts = (JifTypeSystem) ts;

        for (P comp : comps) {
            @SuppressWarnings("unchecked")
            P ci = (P) comp.simplify();

            if (ci.hasVariables() || ci.hasWritersToReaders()) {
                needed.add(ci);
            } else {
                boolean subsumed = false;

                for (Iterator<P> j = needed.iterator(); j.hasNext();) {
                    P cj = j.next();

                    if (cj.hasVariables() || cj.hasWritersToReaders()) {
                        continue;
                    }

                    if (jts.leq(cj, ci)) {
                        subsumed = true;
                        break;
                    }

                    if (jts.leq(ci, cj)) {
                        j.remove();
                    }
                }

                if (!subsumed) needed.add(ci);
            }
        }

        if (needed.equals(meetComponents)) {
            return this;
        }
        if (needed.size() == 1) {
            return needed.iterator().next();
        }

        return constructMeetPolicy(needed, position);
    }

    protected abstract Policy constructMeetPolicy(Set<P> components,
            Position pos);

    private static <P extends Policy> Set<P> flatten(Set<P> comps) {
        // check if there are any meet policies in there.
        boolean needFlattening = false;
        for (P p : comps) {
            if (p instanceof MeetPolicy_c) {
                needFlattening = true;
                break;
            }
        }

        if (!needFlattening) return comps;

        Set<P> c = new LinkedHashSet<P>();
        for (P p : comps) {
            if (p.isBottom()) {
                return Collections.singleton(p);
            }

            if (p instanceof MeetPolicy_c) {
                @SuppressWarnings("unchecked")
                MeetPolicy_c<P> mp = (MeetPolicy_c<P>) p;
                Collection<P> lComps = mp.meetComponents();
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
        for (P L : meetComponents) {
            throwTypes.addAll(L.throwTypes(ts));
        }
        return throwTypes;
    }

    @Override
    public Policy subst(LabelSubstitution substitution)
            throws SemanticException {
        if (meetComponents.isEmpty()) {
            return substitution.substPolicy(this).simplify();
        }
        boolean changed = false;
        Set<P> s = new LinkedHashSet<P>();

        for (P c : meetComponents) {
            @SuppressWarnings("unchecked")
            P newc = (P) c.subst(substitution);
            if (newc != c) changed = true;
            s.add(newc);
        }

        if (!changed) return substitution.substPolicy(this).simplify();

        Policy newMeetPolicy = constructMeetPolicy(flatten(s), position);
        return substitution.substPolicy(newMeetPolicy).simplify();
    }

    @Override
    public boolean hasWritersToReaders() {
        for (P c : meetComponents) {
            if (c.hasWritersToReaders()) return true;
        }
        return false;
    }

    @Override
    public boolean hasVariables() {
        for (P c : meetComponents) {
            if (c.hasVariables()) return true;
        }
        return false;
    }

    @Override
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        JifTypeSystem ts = (JifTypeSystem) A.typeSystem();
        PathMap X = ts.pathMap().N(A.pc()).NV(A.pc());

        if (meetComponents.isEmpty()) {
            return X;
        }

        A = (JifContext) A.pushBlock();

        for (P c : meetComponents) {
            updateContextForComp(lc, A, X);
            PathMap Xc = c.labelCheck(A, lc);
            X = X.join(Xc);
        }
        return X;
    }

    /**
     * Utility method for updating the context for checking a meet component.
     *
     * Useful for overriding in projects like Fabric.
     */
    protected void updateContextForComp(LabelChecker lc, JifContext A,
            PathMap Xprev) {
        A.setPc(Xprev.N(), lc);
    }

    @Override
    public boolean isTop() {
        // top if all policies is top
        for (P c : meetComponents) {
            if (!c.isTop()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isBottom() {
        // bottom if any policy is bottom
        for (P c : meetComponents) {
            if (c.isBottom()) {
                return true;
            }
        }
        return false;
    }
}
