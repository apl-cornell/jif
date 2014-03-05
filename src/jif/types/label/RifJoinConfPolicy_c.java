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
public class RifJoinConfPolicy_c extends Policy_c implements RifJoinConfPolicy {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private final Set<RifReaderPolicy> joinComponents;
    private Integer hashCode = null;

    public RifJoinConfPolicy_c(Set<RifReaderPolicy> components,
            JifTypeSystem ts, Position pos) {
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
        for (RifReaderPolicy c : joinComponents) {
            if (!c.isCanonical()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        for (RifReaderPolicy c : joinComponents) {
            if (!c.isRuntimeRepresentable()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (o instanceof RifJoinConfPolicy_c) {
            RifJoinConfPolicy_c that = (RifJoinConfPolicy_c) o;
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
        for (Iterator<RifReaderPolicy> i = joinComponents.iterator(); i
                .hasNext();) {
            RifReaderPolicy c = i.next();
            s += c.toString(printedLabels);

            if (i.hasNext()) {
                s += " join ";
            }
        }

        return s;
    }

    @Override
    public boolean leq_(Policy p, LabelEnv env, SearchState state) {
        for (RifReaderPolicy pi : joinComponents) {
            if (env.leq(pi, p, state)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Collection<RifReaderPolicy> joinComponents() {
        return Collections.unmodifiableCollection(joinComponents);
    }

    /**
     * @return An equivalent label with fewer components by pulling out
     * less restrictive policies.
     */
    @Override
    protected Policy simplifyImpl() {
        // to be implemented!
        return this;
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        List<Type> throwTypes = new ArrayList<Type>();
        for (RifReaderPolicy L : joinComponents) {
            throwTypes.addAll(L.throwTypes(ts));
        }
        return throwTypes;
    }

    private static Set<RifReaderPolicy> flatten(Set<RifReaderPolicy> comps) {
        // to be implemented
        return comps;
    }

    protected RifJoinConfPolicy constructRifJoinConfPolicy(
            Set<RifReaderPolicy> components, Position pos) {
        return new RifJoinConfPolicy_c(components, (JifTypeSystem) ts, pos);
    }

    @Override
    public Policy subst(LabelSubstitution substitution)
            throws SemanticException {
        if (joinComponents.isEmpty()) {
            return substitution.substPolicy(this).simplify();
        }
        boolean changed = false;
        Set<RifReaderPolicy> s = new LinkedHashSet<RifReaderPolicy>();

        for (RifReaderPolicy c : joinComponents) {
            RifReaderPolicy newc = (RifReaderPolicy) c.subst(substitution);
            if (newc != c) changed = true;
            s.add(newc);
        }

        if (!changed) return substitution.substPolicy(this).simplify();

        Policy newRifJoinConfPolicy =
                constructRifJoinConfPolicy(flatten(s), position);
        return substitution.substPolicy(newRifJoinConfPolicy).simplify();
    }

    @Override
    public boolean hasWritersToReaders() {
        for (RifReaderPolicy c : joinComponents) {
            if (c.hasWritersToReaders()) return true;
        }
        return false;
    }

    @Override
    public boolean hasVariables() {
        for (RifReaderPolicy c : joinComponents) {
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

        for (RifReaderPolicy c : joinComponents) {
            A.setPc(X.N(), lc);
            PathMap Xc = c.labelCheck(A, lc);
            X = X.join(Xc);
        }
        return X;
    }

    @Override
    public boolean isTop() {
        // top if all policies is top
        for (RifReaderPolicy c : joinComponents) {
            if (!c.isTop()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isBottom() {
        // bottom if any policy is bottom
        for (RifReaderPolicy c : joinComponents) {
            if (c.isBottom()) {
                return true;
            }
        }
        return false;
    }

    @Override
    // this might not do exactly what we want!
    public ConfPolicy meet(ConfPolicy p) {
        JifTypeSystem ts = (JifTypeSystem) this.ts;
        return ts.meet(this, p);
    }

    @Override
    public ConfPolicy join(ConfPolicy p) {
        JifTypeSystem ts = (JifTypeSystem) this.ts;
        return ts.join(this, p);
    }

    @Override
    public boolean isBottomConfidentiality() {
        return isBottom();
    }

    @Override
    public boolean isTopConfidentiality() {
        return isTop();
    }

    @Override
    public boolean leq_(ConfPolicy p, LabelEnv env, SearchState state) {
        return leq_((Policy) p, env, state);
    }

    @Override
    public ConfPolicy meet(RifReaderPolicy p) {
        return meet((ConfPolicy) p);
    }

    @Override
    public ConfPolicy join(RifReaderPolicy p) {
        return join((ConfPolicy) p);
    }
}
