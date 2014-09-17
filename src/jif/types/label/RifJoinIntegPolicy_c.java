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
import jif.types.RifFSM;
import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.LabelEnv.SearchState;
import jif.visit.LabelChecker;
import polyglot.ast.Id;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** Represents the meet of a number of policies. 
 */
public class RifJoinIntegPolicy_c extends Policy_c implements
        RifJoinIntegPolicy {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private final Set<IntegPolicy> joinComponents;
    private Integer hashCode = null;

    public RifJoinIntegPolicy_c(Set<IntegPolicy> components, JifTypeSystem ts,
            Position pos) {
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
        for (IntegPolicy c : joinComponents) {
            if (!c.isCanonical()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        for (IntegPolicy c : joinComponents) {
            if (!c.isRuntimeRepresentable()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (o instanceof RifJoinIntegPolicy_c) {
            RifJoinIntegPolicy_c that = (RifJoinIntegPolicy_c) o;
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
        for (Iterator<IntegPolicy> i = joinComponents.iterator(); i.hasNext();) {
            IntegPolicy c = i.next();
            s += c.toString(printedLabels);

            if (i.hasNext()) {
                s += " join ";
            }
        }

        return s;
    }

    @Override
    public boolean leq_(Policy p, LabelEnv env, SearchState state) {
        for (IntegPolicy pi : joinComponents) {
            if (!env.leq(pi, p, state)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Collection<IntegPolicy> joinComponents() {
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
        for (IntegPolicy L : joinComponents) {
            throwTypes.addAll(L.throwTypes(ts));
        }
        return throwTypes;
    }

    private static Set<IntegPolicy> flatten(Set<IntegPolicy> comps) {
        boolean needFlattening = false;
        for (IntegPolicy cp : comps) {
            if (cp instanceof RifJoinIntegPolicy) {
                needFlattening = true;
                break;
            }
        }

        if (!needFlattening) return comps;

        Set<IntegPolicy> c = new LinkedHashSet<IntegPolicy>();
        for (IntegPolicy cp : comps) {
            if (cp.isTop()) {
                return Collections.singleton(cp);
            }

            if (cp instanceof RifJoinIntegPolicy) {
                Collection<IntegPolicy> lComps =
                        ((RifJoinIntegPolicy) cp).joinComponents();
                c.addAll(lComps);
            } else {
                c.add(cp);
            }
        }

        return c;
    }

    @Override
    public RifJoinIntegPolicy takeTransition(Id action) {
        Set<IntegPolicy> s = new LinkedHashSet<IntegPolicy>();
        for (IntegPolicy c : joinComponents) {
            s.add(((RifIntegPolicy) c).takeTransition(action));
        }
        return constructRifJoinIntegPolicy(s, position);
    }

    protected RifJoinIntegPolicy constructRifJoinIntegPolicy(
            Set<IntegPolicy> components, Position pos) {
        return new RifJoinIntegPolicy_c(components, (JifTypeSystem) ts, pos);
    }

    @Override
    public Policy subst(LabelSubstitution substitution)
            throws SemanticException {
        if (joinComponents.isEmpty()) {
            return substitution.substPolicy(this).simplify();
        }
        boolean changed = false;
        Set<IntegPolicy> s = new LinkedHashSet<IntegPolicy>();

        for (IntegPolicy c : joinComponents) {
            RifIntegPolicy newc = (RifIntegPolicy) c.subst(substitution);
            if (newc != c) changed = true;
            s.add(newc);
        }

        if (!changed) return substitution.substPolicy(this).simplify();

        Policy newRifJoinIntegPolicy =
                constructRifJoinIntegPolicy(flatten(s), position);
        return substitution.substPolicy(newRifJoinIntegPolicy).simplify();
    }

    @Override
    public boolean hasWritersToReaders() {
        for (IntegPolicy c : joinComponents) {
            if (c.hasWritersToReaders()) return true;
        }
        return false;
    }

    @Override
    public boolean hasVariables() {
        for (IntegPolicy c : joinComponents) {
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

        for (IntegPolicy c : joinComponents) {
            A.setPc(X.N(), lc);
            PathMap Xc = c.labelCheck(A, lc);
            X = X.join(Xc);
        }
        return X;
    }

    @Override
    public boolean isTop() {
        // top if any policy is top
        for (IntegPolicy c : joinComponents) {
            if (c.isTop()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBottom() {
        // bottom if all policies are bottom
        for (IntegPolicy c : joinComponents) {
            if (!c.isBottom()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isBottomIntegrity() {
        return isBottom();
    }

    @Override
    public boolean isTopIntegrity() {
        return isTop();
    }

    @Override
    public boolean leq_(IntegPolicy p, LabelEnv env, SearchState state) {
        return leq_((Policy) p, env, state);
    }

    @Override
    // this might not do exactly what we want!
    public IntegPolicy meet(IntegPolicy p) {
        JifTypeSystem ts = (JifTypeSystem) this.ts;
        return ts.meet(this, p);
    }

    @Override
    public IntegPolicy join(IntegPolicy p) {
        JifTypeSystem ts = (JifTypeSystem) this.ts;
        return ts.join(this, p);
    }

    @Override
    public IntegPolicy flatten() {
        IntegPolicy temp = null;
        IntegPolicy next = null;

        for (IntegPolicy c : joinComponents) {
            if (c instanceof RifJoinIntegPolicy) {
                next = ((RifJoinIntegPolicy) c).flatten();
            } else {
                next = c;
            }
            if (temp == null) {
                temp = next;
            } else {
                temp = temp.join(next);
            }
        }
        return temp;
    }

    @Override
    public Set<RifFSM> getFSMs() {
        Set<RifFSM> l = new LinkedHashSet<RifFSM>();

        for (IntegPolicy c : joinComponents) {
            if (c instanceof RifWriterPolicy_c) {
                l.add(((RifWriterPolicy_c) c).getFSM());
            } else {
                l.addAll(((RifJoinIntegPolicy) c).getFSMs());
            }
        }
        return l;
    }
}
