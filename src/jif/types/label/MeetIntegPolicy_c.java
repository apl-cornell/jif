package jif.types.label;

import java.util.ArrayList;
import java.util.Collection;

import jif.types.JifTypeSystem;
import jif.types.hierarchy.LabelEnv;
import polyglot.util.Position;
import polyglot.util.TypedList;


/** Represents the meet of a number of integrity policies. 
 */
public class MeetIntegPolicy_c extends MeetPolicy_c implements IntegPolicy {

    public MeetIntegPolicy_c(Collection components, JifTypeSystem ts, Position pos) {
        super(components, ts, pos);
        // check that all the components are integrity policies
        TypedList.check(new ArrayList(components), IntegPolicy.class);
    }

    protected Policy constructMeetPolicy(Collection components, Position pos) {
        return new MeetIntegPolicy_c(components, (JifTypeSystem)ts, pos);
    }

    public boolean isBottomIntegrity() {
        return isBottom();
    }

    public boolean isTopIntegrity() {
        return isTop();
    }

    public boolean leq_(IntegPolicy p, LabelEnv env) {
        return leq_((Policy)p, env);
    }
    public IntegPolicy meet(IntegPolicy p) {
        JifTypeSystem ts = (JifTypeSystem)this.ts;
        return ts.meet(this, p);
    }
    public IntegPolicy join(IntegPolicy p) {
        JifTypeSystem ts = (JifTypeSystem)this.ts;
        return ts.join(this, p);
    }
}