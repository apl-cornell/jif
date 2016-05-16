package jif.types.label;

import java.util.Set;

import jif.types.JifTypeSystem;
import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.LabelEnv.SearchState;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** Represents the meet of a number of integrity policies.
 */
public class MeetIntegPolicy_c extends MeetPolicy_c<IntegPolicy>
        implements IntegPolicy {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public MeetIntegPolicy_c(Set<IntegPolicy> components, JifTypeSystem ts,
            Position pos) {
        super(components, ts, pos);
    }

    @Override
    protected Policy constructMeetPolicy(Set<IntegPolicy> components,
            Position pos) {
        JifTypeSystem jts = (JifTypeSystem) ts;
        return jts.meetIntegPolicy(pos, components);
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
    public IntegPolicy meet(IntegPolicy p) {
        JifTypeSystem ts = (JifTypeSystem) this.ts;
        return ts.meet(this, p);
    }

    @Override
    public IntegPolicy join(IntegPolicy p) {
        JifTypeSystem ts = (JifTypeSystem) this.ts;
        return ts.join(this, p);
    }
}
