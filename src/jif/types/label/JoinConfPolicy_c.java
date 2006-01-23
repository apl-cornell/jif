package jif.types.label;

import java.util.ArrayList;
import java.util.Collection;

import jif.types.JifTypeSystem;
import jif.types.hierarchy.LabelEnv;
import polyglot.util.Position;
import polyglot.util.TypedList;


/** Represents the meet of a number of integrity policies. 
 */
public class JoinConfPolicy_c extends JoinPolicy_c implements ConfPolicy {

    public JoinConfPolicy_c(Collection components, JifTypeSystem ts, Position pos) {
        super(components, ts, pos);
        // check that all the components are confidentiality policies
        TypedList.check(new ArrayList(components), ConfPolicy.class);
    }

    protected Policy constructJoinPolicy(Collection components, Position pos) {
        return new JoinConfPolicy_c(components, (JifTypeSystem)ts, pos);
    }
    public boolean isBottomConfidentiality() {
        return isBottom();
    }

    public boolean isTopConfidentiality() {
        return isTop();
    }

    public boolean leq_(ConfPolicy p, LabelEnv env) {
        return leq_((Policy)p, env);
    }    
    
    public ConfPolicy meet(ConfPolicy p) {
        JifTypeSystem ts = (JifTypeSystem)this.ts;
        return ts.meet(this, p);
    }
    public ConfPolicy join(ConfPolicy p) {
        JifTypeSystem ts = (JifTypeSystem)this.ts;
        return ts.join(this, p);
    }
    
}