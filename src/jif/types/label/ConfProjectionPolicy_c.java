package jif.types.label;

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
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** The confidentiality projection of a (non meet, join or pair) label.
 */
public class ConfProjectionPolicy_c extends Policy_c
        implements ConfProjectionPolicy {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private final Label label;

    public ConfProjectionPolicy_c(Label label, JifTypeSystem ts, Position pos) {
        super(ts, pos);
        this.label = label;
    }

    public Label label() {
        return label;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public boolean isCanonical() {
        return label.isCanonical();
    }

    @Override
    public boolean isRuntimeRepresentable() {
        return label.isRuntimeRepresentable();
    }

    @Override
    protected Policy simplifyImpl() {
        return this;
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (o instanceof ConfProjectionPolicy_c) {
            ConfProjectionPolicy_c that = (ConfProjectionPolicy_c) o;
            return (this.label == that.label
                    || (this.label != null && this.label.equals(that.label)));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return label.hashCode();
    }

    @Override
    public boolean leq_(ConfPolicy p, LabelEnv env, SearchState state) {
        if (p instanceof ConfProjectionPolicy_c) {
            return env.leq(this.label(), ((ConfProjectionPolicy_c) p).label(),
                    state);
        }
        if (p.isTopConfidentiality()) return true;
        Label ub = env.findUpperBound(this.label);
        return env.leq(ub.confProjection(), p, state);
    }

    @Override
    public String toString(Set<Label> printedLabels) {
        return "C(" + label.componentString(printedLabels) + ")";
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        return label.throwTypes(ts);
    }

    @Override
    public Policy subst(LabelSubstitution substitution)
            throws SemanticException {

        Label newLabel = label.subst(substitution);
        if (newLabel == label) {
            return substitution.substPolicy(this).simplify();
        }

        JifTypeSystem ts = (JifTypeSystem) typeSystem();
        Policy newPolicy = ts.confProjection(newLabel);
        return substitution.substPolicy(newPolicy).simplify();
    }

    @Override
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        return label.labelCheck(A, lc);
    }

    @Override
    public boolean isBottomConfidentiality() {
        return label.isBottom();
    }

    @Override
    public boolean isTopConfidentiality() {
        return label.isTop();
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
    public boolean hasWritersToReaders() {
        return label.hasWritersToReaders();
    }

    @Override
    public boolean hasVariables() {
        return label.hasVariables();
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
