package jif.types.label;

import java.util.LinkedHashSet;
import java.util.LinkedList;
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
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>PolicyLabel</code> interface.
 */
public class RifWriterPolicy_c extends Policy_c implements RifIntegPolicy {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private RifFSM fsm;

    public RifWriterPolicy_c(RifFSM fsm, JifTypeSystem ts, Position pos) {
        super(ts, pos);
        this.fsm = fsm;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public RifFSM getFSM() {
        return this.fsm;
    }

    @Override
    public Set<RifFSM> getFSMs() {
        Set<RifFSM> l = new LinkedHashSet<RifFSM>();
        l.add(this.fsm);
        return l;
    }

    @Override
    public boolean isCanonical() {
        return this.fsm.isCanonical();
    }

    @Override
    public boolean isRuntimeRepresentable() {
        return this.fsm.isRuntimeRepresentable();
    }

    @Override
    public RifIntegPolicy takeTransition(Id action) {
        RifFSM newfsm = this.fsm.takeTransition(action);
        return new RifWriterPolicy_c(newfsm, (JifTypeSystem) this.ts,
                this.position);
    }

    @Override
    protected Policy simplifyImpl() {
        return this;
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        List<String> visited = new LinkedList<String>();
        if (this == o) return true;
        if (o instanceof RifWriterPolicy_c) {
            return this.fsm
                    .equalsFSM(((RifWriterPolicy_c) o).getFSM(), visited);
        }
        if (o instanceof RifJoinIntegPolicy_c) {
            throw new UnsupportedOperationException(
                    "Try to compare RifReaderPolicy with RifJoinConfPolicy.");
        }
        return false;
    }

    @Override
    public int hashCode() {
        // return (owner == null ? 0 : owner.hashCode()) ^ reader.hashCode()
        //         ^ 948234;
        return 948234;
    }

    @Override
    public boolean leq_(IntegPolicy p, LabelEnv env, SearchState state) {
        List<String> visited = new LinkedList<String>();
        if (this.isTopIntegrity() || p.isBottomIntegrity()) return true;
        if (p instanceof RifWriterPolicy_c) {
            return ((RifWriterPolicy_c) p).getFSM().leqFSM(this.fsm, visited);
        }
        if (p instanceof RifJoinIntegPolicy_c) {
            throw new UnsupportedOperationException(
                    "Try to compare RifWriterPolicy with RifJoinIntegPolicy.");
        }
        return false;
    }

    @Override
    public String toString(Set<Label> printedLabels) {
        return this.fsm.toString();
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        return this.fsm.throwTypes(ts);
    }

    @Override
    public Policy subst(LabelSubstitution substitution)
            throws SemanticException {
        RifFSM newfsm;
        newfsm = this.fsm.subst(substitution);
        if (newfsm == null) {
            return substitution.substPolicy(this);
        }

        JifTypeSystem ts = (JifTypeSystem) typeSystem();
        RifIntegPolicy newPolicy = ts.rifwriterPolicy(this.position(), newfsm);
        return substitution.substPolicy(newPolicy);
    }

    @Override
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        return this.fsm.labelCheck(A, lc);
    }

    @Override
    public boolean isBottomIntegrity() {
        return this.fsm.isTopConfidentiality();
    }

    @Override
    public boolean isTopIntegrity() {
        return this.fsm.isBottomConfidentiality();
    }

    @Override
    public boolean isTop() {
        return isTopIntegrity();
    }

    @Override
    public boolean isBottom() {
        return isBottomIntegrity();
    }

    @Override
    public IntegPolicy meet(IntegPolicy p) {
        JifTypeSystem ts = (JifTypeSystem) this.ts;
        return ts.meet(this, p);
    }

    @Override
    public IntegPolicy join(IntegPolicy p) {
        JifTypeSystem ts = (JifTypeSystem) this.ts;
        IntegPolicy newpol;
        if (p instanceof RifWriterPolicy_c) {
            newpol = ts.join(this, (RifWriterPolicy_c) p);
        } else if (p instanceof RifIntegPolicy) {
            newpol = ts.join(this, p);
        } else {
            newpol = ts.join(this, p);
        }
        return newpol;
    }
}
