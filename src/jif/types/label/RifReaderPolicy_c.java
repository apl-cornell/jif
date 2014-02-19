package jif.types.label;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import jif.types.PathMap;
import jif.types.RifComponent;
import jif.types.RifFSM;
import jif.types.RifFSM_c;
import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.LabelEnv.SearchState;
import jif.visit.LabelChecker;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>PolicyLabel</code> interface.
 */
public class RifReaderPolicy_c extends Policy_c implements RifReaderPolicy {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private final List<RifComponent> components;
    private RifFSM fsm;

    //maybe components and fsm contain duplicate information

    public RifReaderPolicy_c(List<RifComponent> components, JifTypeSystem ts,
            Position pos) {
        super(ts, pos);
        this.components = components;
        this.fsm = new RifFSM_c(components);
    }

    @Override
    public List<RifComponent> components() {
        return this.components;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public RifFSM getFSM() {
        return this.fsm;
    }

    @Override
    public boolean isCanonical() {
        List<String> visited = new LinkedList<String>();
        return this.fsm.isCanonical(visited);
    }

    @Override
    public boolean isRuntimeRepresentable() {
        List<String> visited = new LinkedList<String>();
        return this.fsm.isRuntimeRepresentable(visited);
    }

    @Override
    protected Policy simplifyImpl() {
        return this;
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        List<String> visited = new LinkedList<String>();
        if (this == o) return true;
        return this.fsm.equalsFSM(((RifReaderPolicy) o).getFSM(), visited);
    }

    @Override
    public int hashCode() {
        // return (owner == null ? 0 : owner.hashCode()) ^ reader.hashCode()
        //         ^ 948234;
        return 0;
    }

    @Override
    public boolean leq_(ConfPolicy p, LabelEnv env, SearchState state) {
        List<String> visited = new LinkedList<String>();
        if (this.isBottomConfidentiality() || p.isTopConfidentiality())
            return true;
        if (p instanceof RifReaderPolicy) {
            return this.fsm.leqFSM(((RifReaderPolicy) p).getFSM(), visited);
        }
        return false;
    }

    @Override
    public String toString(Set<Label> printedLabels) {
        List<String> visited = new LinkedList<String>();
        return this.fsm.toString(visited);
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        List<String> visited = new LinkedList<String>();
        return this.fsm.throwTypes(ts, visited);
    }

    //fix it
    @Override
    public Policy subst(LabelSubstitution substitution)
            throws SemanticException {
        boolean changed = false;
        List<RifComponent> l = new LinkedList<RifComponent>();

        for (RifComponent c : this.components) {
            RifComponent newcomponent = c.subst(substitution);
            l.add(newcomponent);
            if (newcomponent != c) changed = true;
        }
        if (!changed) return substitution.substPolicy(this);

        JifTypeSystem ts = (JifTypeSystem) typeSystem();
        RifReaderPolicy newPolicy = ts.rifreaderPolicy(this.position(), l);
        return substitution.substPolicy(newPolicy);
    }

    @Override
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        // check each principal in turn.
        PathMap X;
        PathMap Xtot = null; //or bottom
        for (RifComponent c : this.components) {
            X = c.labelCheck(A, lc);
            A.setPc(X.N(), lc);
            Xtot = Xtot.join(X);
        }
        return Xtot;
    }

    @Override
    public boolean isBottomConfidentiality() {
        List<String> visited = new LinkedList<String>();
        return this.fsm.isBottomConfidentiality(visited);
    }

    @Override
    public boolean isTopConfidentiality() {
        List<String> visited = new LinkedList<String>();
        return this.fsm.isTopConfidentiality(visited);
    }

    @Override
    public boolean isTop() {
        return isTopConfidentiality();
    }

    @Override
    public boolean isBottom() {
        return isBottomConfidentiality();
    }

    @Override
    public ConfPolicy meet(ConfPolicy p) {
        JifTypeSystem ts = (JifTypeSystem) this.ts;
        return ts.meet(this, p);
    }

    @Override
    public ConfPolicy join(ConfPolicy p) {
        JifTypeSystem ts = (JifTypeSystem) this.ts;
        return ts.join(this, p);
    }
}