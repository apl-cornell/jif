package jif.types;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import polyglot.ast.Id;
import polyglot.ast.Id_c;

public class RifFSM_c implements RifFSM {

    protected HashMap<Id, RifFSMstate> states;
    protected RifFSMstate current;
    // allPossibleActions contains all the actions that appear in the program.
    // Somehow this list should be initialized when the whole program is parsed.
    private LinkedList<Id> allPossibleActions;

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

        allPossibleActions = new LinkedList<Id>();
        int i;
        for (i = 0; i < 100; i++) {
            allPossibleActions.add(new Id_c(null, "f" + Integer.toString(i)));
        }
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

    @Override
    public boolean equalsFSM(RifFSM other, List<String> visited) {
        String pair = this.current.name() + "&" + other.currentState().name();
        List<String> newvisited = new LinkedList<String>();

        if (visited.contains(pair)) {
            return true;
        }
        for (String s : visited) {
            newvisited.add(s);
        }
        newvisited.add(pair);
        if (this.currentState().equalsFSM(other.currentState())) {
            for (Id action : allPossibleActions) {
                if (!this.takeTransition(action).equalsFSM(
                        other.takeTransition(action), newvisited)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean leqFSM(RifFSM other, List<String> visited) {
        String pair = this.current.name() + "&" + other.currentState().name();
        List<String> newvisited = new LinkedList<String>();

        if (visited.contains(pair)) {
            return true;
        }
        for (String s : visited) {
            newvisited.add(s);
        }
        newvisited.add(pair);
        if (this.currentState().leqFSM(other.currentState())) {
            for (Id action : allPossibleActions) {
                if (!this.takeTransition(action).leqFSM(
                        other.takeTransition(action), newvisited)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

}
