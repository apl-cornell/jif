package jif.types;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import polyglot.ast.Id;

public class RifFSM_c implements RifFSM {

    protected HashMap<Id, RifFSMstate> states;
    protected RifFSMstate current;

    public RifFSM_c(List<RifComponent> components) {
        Id currName = null;
        states = new HashMap<Id, RifFSMstate>();
        List<RifState> ls = new LinkedList<RifState>();
        List<RifTransition> lt = new LinkedList<RifTransition>();

        for (RifComponent c : components) {
            if (c instanceof RifState) {
                ls.add((RifState) c);
                if (((RifState) c).isCurrent()) {
                    currName = ((RifState) c).name();
                }
            } else if (c instanceof RifTransition) {
                lt.add((RifTransition) c);
            }
        }

        for (RifState s : ls) {
            RifFSMstate state = new RifFSMstate_c(s);
            states.put(s.name(), state);
        }

        for (RifTransition t : lt) {
            RifFSMstate lstate = states.get(t.lstate());
            RifFSMstate rstate = states.get(t.rstate());
            lstate.setTransition(t.name(), rstate);
        }

        this.current = states.get(currName);
    }

    public RifFSM_c(HashMap<Id, RifFSMstate> states, RifFSMstate current) {
        this.states = states;
        this.current = current;
    }

    @Override
    public RifFSMstate currentState() {
        return this.current;
    }

    @Override
    public RifFSM takeTransition(Id action) {
        RifFSM newfsm;
        RifFSMstate nextState = this.current.getNextState(action);
        newfsm = new RifFSM_c(this.states, nextState);
        return newfsm;
    }
}
