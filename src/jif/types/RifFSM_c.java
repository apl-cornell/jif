package jif.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Id;
import polyglot.ast.Id_c;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.SerialVersionUID;

public class RifFSM_c implements RifFSM {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected Map<String, RifFSMstate> states;
    protected RifFSMstate current;
    // allPossibleActions contains all the actions that appear in the program.
    // Somehow this list should be initialized when the whole program is parsed.
    private LinkedList<Id> allPossibleActions;

    public RifFSM_c(List<RifComponent> components) {
        Map<String, RifFSMstate> states;

        String currName = null;
        states = new HashMap<String, RifFSMstate>();
        List<RifState> ls = new LinkedList<RifState>();
        List<RifTransition> lt = new LinkedList<RifTransition>();

        for (RifComponent c : components) {
            if (c instanceof RifState) {
                ls.add((RifState) c);
                if (((RifState) c).isCurrent()) {
                    currName = ((RifState) c).name().id();
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
            lstate.setTransition(t.name().id(), rstate);
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
    public Map<String, RifFSMstate> states() {
        return this.states;
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
    public boolean isCanonical() {
        for (Entry<String, RifFSMstate> pair : states.entrySet()) {
            if (!pair.getValue().isCanonical()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        Iterator<Entry<String, RifFSMstate>> it =
                this.states.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, RifFSMstate> pairs = it.next();
            if (!pairs.getValue().isRuntimeRepresentable()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        List<Type> throwTypes = new ArrayList<Type>();
        Iterator<Entry<String, RifFSMstate>> it =
                this.states.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, RifFSMstate> pairs = it.next();
            throwTypes.addAll(pairs.getValue().throwTypes(ts));
        }
        return throwTypes;
    }

    @Override
    public boolean isBottomConfidentiality() {
        Iterator<Entry<String, RifFSMstate>> it =
                this.states.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, RifFSMstate> pairs = it.next();
            if (!pairs.getValue().isBottomConfidentiality()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isTopConfidentiality() {
        Iterator<Entry<String, RifFSMstate>> it =
                this.states.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, RifFSMstate> pairs = it.next();
            if (!pairs.getValue().isTopConfidentiality()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {

        StringBuffer sb = new StringBuffer();

        Iterator<Entry<String, RifFSMstate>> it =
                this.states.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, RifFSMstate> pairs = it.next();
            sb.append(pairs.getValue().toString(
                    pairs.getKey() == this.current.name().id()));
            if (it.hasNext()) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    @Override
    public RifFSM subst(LabelSubstitution substitution)
            throws SemanticException {
        RifFSMstate state;
        boolean changed = false;
        RifFSMstate current = null;
        HashMap<String, RifFSMstate> l = new HashMap<String, RifFSMstate>();
        Iterator<Entry<String, RifFSMstate>> it =
                this.states.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, RifFSMstate> pairs = it.next();
            List<Principal> principals = pairs.getValue().subst(substitution);
            if (principals != null) {
                changed = true;
                state =
                        new RifFSMstate_c(pairs.getValue().name(), principals,
                                null);
            } else {
                state =
                        new RifFSMstate_c(pairs.getValue().name(), pairs
                                .getValue().principals(), null);
            }
            l.put(pairs.getValue().name().id(), state);
        }
        if (changed) {
            Iterator<Entry<String, RifFSMstate>> it2 =
                    this.states.entrySet().iterator();
            while (it2.hasNext()) {
                Entry<String, RifFSMstate> pairs = it2.next();
                HashMap<String, RifFSMstate> transitions =
                        pairs.getValue().getTransitions();
                Iterator<Entry<String, RifFSMstate>> transIt =
                        transitions.entrySet().iterator();
                while (transIt.hasNext()) {
                    Entry<String, RifFSMstate> trans = transIt.next();
                    RifFSMstate reachedstate =
                            l.get(trans.getValue().name().id());
                    l.get(pairs.getKey()).setTransition(trans.getKey(),
                            reachedstate);
                }
                if (pairs.getKey() == this.current.name().id()) {
                    current = l.get(pairs.getKey());
                }
            }
            return new RifFSM_c(l, current);
        }
        return null;
    }

    @Override
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        Iterator<Entry<String, RifFSMstate>> it =
                this.states.entrySet().iterator();
        PathMap X;
        PathMap Xtot = null; //or bottom
        while (it.hasNext()) {
            Entry<String, RifFSMstate> pairs = it.next();
            X = pairs.getValue().labelCheck(A, lc);
            A.setPc(X.N(), lc);
            Xtot = Xtot.join(X);
        }
        return Xtot;
    }

}
