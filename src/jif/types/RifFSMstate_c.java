package jif.types;

import java.util.HashMap;
import java.util.List;

import jif.types.principal.Principal;
import polyglot.ast.Id;

public class RifFSMstate_c implements RifFSMstate {

    private Id name;
    private List<Principal> principals;
    protected HashMap<Id, RifFSMstate> transitions;

    public RifFSMstate_c(RifState s) {
        this.name = s.name();
        this.principals = s.principals();
        this.transitions = new HashMap<Id, RifFSMstate>();
    }

    @Override
    public void setTransition(Id transName, RifFSMstate rstate) {
        this.transitions.put(transName, rstate);
    }

    @Override
    public RifFSMstate getNextState(Id action) {
        RifFSMstate nextState = this.transitions.get(action);
        if (nextState == null) {
            return this;
        } else {
            return nextState;
        }
    }

    @Override
    public List<Principal> principals() {
        return this.principals;
    }

    @Override
    public boolean equalsFSM(RifFSMstate other) {
        List<Principal> set1 = this.principals;
        List<Principal> set2 = other.principals();
        return set1.containsAll(set2) && set2.containsAll(set1);
    }
}
