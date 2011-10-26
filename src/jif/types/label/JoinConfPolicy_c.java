package jif.types.label;

import java.util.Set;

import jif.types.JifTypeSystem;
import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.LabelEnv.SearchState;
import polyglot.util.Position;


/** Represents the join of a number of confidentiality policies. 
 */
public class JoinConfPolicy_c extends JoinPolicy_c<ConfPolicy> implements
        ConfPolicy {

    public JoinConfPolicy_c(Set<ConfPolicy> components, JifTypeSystem ts,
            Position pos) {
        super(components, ts, pos);
    }

    @Override
    protected Policy constructJoinPolicy(Set<ConfPolicy> components,
            Position pos) {
        return new JoinConfPolicy_c(components, (JifTypeSystem)ts, pos);
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
        return leq_((Policy)p, env, state);
    }    
    
    @Override
    public ConfPolicy meet(ConfPolicy p) {
        JifTypeSystem ts = (JifTypeSystem)this.ts;
        return ts.meet(this, p);
    }
    @Override
    public ConfPolicy join(ConfPolicy p) {
        JifTypeSystem ts = (JifTypeSystem)this.ts;
        return ts.join(this, p);
    }
    
}
