package jif.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import polyglot.ast.Id;
import polyglot.ast.Id_c;
import polyglot.types.Type;
import polyglot.types.TypeSystem;

public class RifFSM_c implements RifFSM {

    protected Map<String, RifFSMstate> states;
    protected RifFSMstate current;
    // allPossibleActions contains all the actions that appear in the program.
    // Somehow this list should be initialized when the whole program is parsed.
    private LinkedList<Id> allPossibleActions;

    public RifFSM_c(List<RifComponent> components) {
        Map<String, RifFSMstate> states;

        Id currName = null;
        states = new HashMap<String, RifFSMstate>();
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
            states.put(s.name().id(), state);
        }

        this.states = Collections.unmodifiableMap(states);

        for (RifTransition t : lt) {

            RifFSMstate lstate = states.get(t.lstate().id());
            RifFSMstate rstate = states.get(t.rstate().id());
            lstate.setTransition(t.name(), rstate);
        }

        this.current = states.get(currName);

        allPossibleActions = new LinkedList<Id>();
        int i;
        for (i = 0; i < 100; i++) {
            allPossibleActions.add(new Id_c(null, "f" + Integer.toString(i)));
        }
    }

    public RifFSM_c(Map<String, RifFSMstate> states, RifFSMstate current) {
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
        String pair =
                this.current.name().id() + "&"
                        + other.currentState().name().id();
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
        String pair =
                this.current.name().id() + "&"
                        + other.currentState().name().id();
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

    @Override
    public boolean isCanonical(List<String> visited) {
        String name = this.current.name().id();
        List<String> newvisited = new LinkedList<String>();

        if (visited.contains(name)) {
            return true;
        }
        for (String s : visited) {
            newvisited.add(s);
        }
        newvisited.add(name);
        if (this.currentState().isCanonical()) {
            for (Id action : allPossibleActions) {
                if (!this.takeTransition(action).isCanonical(newvisited)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isRuntimeRepresentable(List<String> visited) {
        String name = this.current.name().id();
        List<String> newvisited = new LinkedList<String>();

        if (visited.contains(name)) {
            return true;
        }
        for (String s : visited) {
            newvisited.add(s);
        }
        newvisited.add(name);
        if (this.currentState().isRuntimeRepresentable()) {
            for (Id action : allPossibleActions) {
                if (!this.takeTransition(action).isRuntimeRepresentable(
                        newvisited)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts, List<String> visited) {
        List<Type> throwTypes = new ArrayList<Type>();
        String name = this.current.name().id();
        List<String> newvisited = new LinkedList<String>();

        if (visited.contains(name)) {
            return null;
        }
        for (String s : visited) {
            newvisited.add(s);
        }
        newvisited.add(name);
        throwTypes.addAll(this.currentState().throwTypes(ts));
        for (Id action : allPossibleActions) {
            throwTypes.addAll(this.takeTransition(action).throwTypes(ts,
                    newvisited));
        }
        return throwTypes;
    }

    @Override
    public boolean isBottomConfidentiality(List<String> visited) {
        String name = this.current.name().id();
        List<String> newvisited = new LinkedList<String>();

        if (visited.contains(name)) {
            return true;
        }
        for (String s : visited) {
            newvisited.add(s);
        }
        newvisited.add(name);
        if (this.currentState().isBottomConfidentiality()) {
            for (Id action : allPossibleActions) {
                if (!this.takeTransition(action).isBottomConfidentiality(
                        newvisited)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isTopConfidentiality(List<String> visited) {
        String name = this.current.name().id();
        List<String> newvisited = new LinkedList<String>();

        if (visited.contains(name)) {
            return true;
        }
        for (String s : visited) {
            newvisited.add(s);
        }
        newvisited.add(name);
        if (this.currentState().isTopConfidentiality()) {
            for (Id action : allPossibleActions) {
                if (!this.takeTransition(action).isTopConfidentiality(
                        newvisited)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString(List<String> visited) {
        StringBuffer sb = new StringBuffer();
        String name = this.current.name().id();
        List<String> newvisited = new LinkedList<String>();

        if (visited.contains(name)) {
            return null;
        }
        for (String s : visited) {
            newvisited.add(s);
        }

        sb.append(this.currentState().toString(visited == null));

        for (Id action : allPossibleActions) {
            String temp = this.takeTransition(action).toString(newvisited);
            if (temp != null) {
                sb.append(",");
                sb.append(temp);
            }
        }
        return sb.toString();
    }
}
