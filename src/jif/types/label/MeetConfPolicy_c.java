package jif.types.label;

import java.util.Set;

import jif.types.JifTypeSystem;
import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.LabelEnv.SearchState;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** Represents the meet of a number of confidentiality policies.
 */
public class MeetConfPolicy_c extends MeetPolicy_c<ConfPolicy>
        implements MeetConfPolicy {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public MeetConfPolicy_c(Set<ConfPolicy> components, JifTypeSystem ts,
            Position pos) {
        super(components, ts, pos);
    }

    @Override
    protected Policy constructMeetPolicy(Set<ConfPolicy> components,
            Position pos) {
        JifTypeSystem jts = (JifTypeSystem) ts;
        return jts.meetConfPolicy(pos, components);
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
