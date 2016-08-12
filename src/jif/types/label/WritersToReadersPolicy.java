package jif.types.label;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class WritersToReadersPolicy extends Policy_c implements ConfPolicy {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected IntegPolicy integPol;

    public WritersToReadersPolicy(IntegPolicy ip, JifTypeSystem ts, Position pos) {
        super(ts, pos);
        this.integPol = ip;
    }

    public IntegPolicy integPol() {
      return integPol;
    }

    @Override
    public boolean isBottomConfidentiality() {
        return integPol.isTopIntegrity();
    }

    @Override
    public boolean isTopConfidentiality() {
        return integPol.isBottomIntegrity();
    }

    @Override
    public boolean leq_(ConfPolicy p, LabelEnv env, LabelEnv.SearchState state) {
        if (p.isTopConfidentiality())
          return true;

        if (p instanceof WritersToReadersPolicy) {
            WritersToReadersPolicy other = (WritersToReadersPolicy) p;
            return env.leq(other.integPol(), integPol(), state);
        }

        // Not sure we can do more?
        return false;
    }

    @Override
    public ConfPolicy join(ConfPolicy p) {
        return typeSystem().join((ConfPolicy) simplify(), p);
    }

    @Override
    public ConfPolicy meet(ConfPolicy p) {
        return typeSystem().meet((ConfPolicy) simplify(), p);
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (!(o instanceof WritersToReadersPolicy)) {
            return false;
        }
        WritersToReadersPolicy that = (WritersToReadersPolicy) o;
        return (this.integPol.equals(that.integPol()));
    }

    @Override
    public String toString(Set<Label> printedLabels) {
        return "W2R(" + integPol.toString(printedLabels) + ")";
    }

    @Override
    protected Policy simplifyImpl() {
        return simplifyIntegToConf(integPol);
    }

    protected static ConfPolicy simplifyIntegToConf(IntegPolicy pol) {
        JifTypeSystem ts = (JifTypeSystem) pol.typeSystem();
        pol = (IntegPolicy) pol.simplify();
        if (pol instanceof WriterPolicy) {
            WriterPolicy wp = (WriterPolicy) pol;
            return (ConfPolicy) ts.readerPolicy(wp.position(), wp.owner(),
                wp.writer()).simplify();
        }
        if (pol instanceof JoinIntegPolicy_c) {
            @SuppressWarnings("unchecked")
            JoinPolicy_c<IntegPolicy> jp = (JoinPolicy_c<IntegPolicy>) pol;
            Set<ConfPolicy> newPols =
                    new HashSet<ConfPolicy>(jp.joinComponents().size());
            for (IntegPolicy ip : jp.joinComponents()) {
                ConfPolicy cp = simplifyIntegToConf(ip);
                newPols.add(cp);
            }
            return (ConfPolicy) ts.meetConfPolicy(jp.position(), newPols).simplify();
        }
        if (pol instanceof MeetIntegPolicy_c) {
            @SuppressWarnings("unchecked")
            MeetPolicy_c<IntegPolicy> mp = (MeetPolicy_c<IntegPolicy>) pol;
            Set<ConfPolicy> newPols =
                    new HashSet<ConfPolicy>(mp.meetComponents().size());
            for (IntegPolicy ip : mp.meetComponents()) {
                ConfPolicy cp = simplifyIntegToConf(ip);
                newPols.add(cp);
            }
            return (ConfPolicy) ts.joinConfPolicy(mp.position(), newPols).simplify();
        }
        return new WritersToReadersPolicy(pol, ts, pol.position());
    }

    @Override
    public boolean isRuntimeRepresentable() {
        return integPol.isRuntimeRepresentable();
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        return integPol.throwTypes(ts);
    }

    @Override
    public Policy subst(LabelSubstitution substitution)
            throws SemanticException {
        WritersToReadersPolicy p = this;
        IntegPolicy newInteg = (IntegPolicy) p.integPol.subst(substitution);

        if (newInteg != p.integPol) {
            JifTypeSystem ts = typeSystem();
            p = ts.writersToReadersPolicy(p.position(), newInteg);
        }
        return substitution.substPolicy(p);
    }

    @Override
    public JifTypeSystem typeSystem() {
      return (JifTypeSystem) ts;
    }

    @Override
    public boolean isSingleton() {
        return integPol.isSingleton();
    }

    @Override
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        return integPol.labelCheck(A, lc);
    }

    @Override
    public boolean isBottom() {
        return integPol.isTop();
    }

    @Override
    public boolean isCanonical() {
        return integPol.isCanonical();
    }

    @Override
    public boolean isTop() {
        return integPol.isBottom();
    }

    @Override
    public boolean hasWritersToReaders() {
        return integPol.hasWritersToReaders();
    }

    @Override
    public boolean hasVariables() {
        return integPol.hasVariables();
    }
}
