package jif.lang;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jif.lang.PrincipalUtil.DelegationPair;
import polyglot.ast.Id;
import polyglot.ast.Id_c;

public class RifReaderPolicy extends AbstractPolicy implements ConfPolicy {

    protected Map<String, RifFSMstate> states;
    protected RifFSMstate current;
    // allPossibleActions contains all the actions that appear in the program.
    // Somehow this list should be initialized when the whole program is parsed.
    private LinkedList<Id> allPossibleActions;

    public RifReaderPolicy(LabelUtil labelUtil) {
        super(labelUtil);
        states = new HashMap<String, RifFSMstate>();
        allPossibleActions = new LinkedList<Id>();
        int i;
        for (i = 0; i < 100; i++) {
            allPossibleActions.add(new Id_c(null, "f" + Integer.toString(i)));
        }
    }

    public RifReaderPolicy(LabelUtil labelUtil,
            Map<String, RifFSMstate> states, RifFSMstate current) {
        super(labelUtil);
        this.states = states;
        this.current = current;

        allPossibleActions = new LinkedList<Id>();
        int i;
        for (i = 0; i < 100; i++) {
            allPossibleActions.add(new Id_c(null, "f" + Integer.toString(i)));
        }
    }

    public RifReaderPolicy addstate(String stateId, String current,
            List<Principal> principals) {

        HashMap<String, RifFSMstate> transitions =
                new HashMap<String, RifFSMstate>();
        RifFSMstate state =
                new RifFSMstate(new Id_c(null, stateId), principals,
                        transitions);
        states.put(stateId, state);
        if (current == "true") {
            this.current = state;
        }
        return this;

    }

    public RifReaderPolicy addtransition(String transition, String state1,
            String state2) {

        for (Entry<String, RifFSMstate> st : states.entrySet()) {
            if (st.getKey() == state1) {
                for (Entry<String, RifFSMstate> st2 : states.entrySet()) {
                    if (st2.getKey() == state2) {
                        st.getValue().setTransition(transition, st2.getValue());
                        break;
                    }
                }
                break;
            }
        }
        return this;
    }

    public RifFSMstate currentState() {
        return this.current;
    }

    @Override
    public boolean relabelsTo(Policy p, Set<DelegationPair> s) {
        if (this == p || this.equals(p)) return true;

        if (p instanceof JoinConfPolicy) {
            JoinPolicy jp = (JoinPolicy) p;
            //conservative checking
            for (Policy pi : jp.joinComponents()) {
                if (labelUtil.relabelsTo(this, pi, s)) return true;
            }
            return false;
        } else if (p instanceof MeetConfPolicy) {
            return false; //do we need to fill it???
        } else if (!(p instanceof ReaderPolicy)) return false;

        RifReaderPolicy pp = (RifReaderPolicy) p;

        return leqFSM(pp, new LinkedList<String>());
    }

    @Override
    public int hashCode() {
        return 5555678;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof RifReaderPolicy)) {
            return false;
        }

        RifReaderPolicy policy = (RifReaderPolicy) o;

        return equalsFSM(policy, new LinkedList<String>());
    }

    public boolean equalsFSM(RifReaderPolicy pol, List<String> visited) {
        String pair =
                this.current.name().id() + "&" + pol.currentState().name().id();
        List<String> newvisited = new LinkedList<String>();

        if (visited.contains(pair)) {
            return true;
        }
        for (String s : visited) {
            newvisited.add(s);
        }
        newvisited.add(pair);
        if (this.currentState().equals(pol.currentState())) {
            for (Id action : allPossibleActions) {
                if (!this.takeTransition(action).equalsFSM(
                        pol.takeTransition(action), newvisited)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public boolean leqFSM(RifReaderPolicy pol, List<String> visited) {
        String pair =
                this.current.name().id() + "&" + pol.currentState().name().id();
        List<String> newvisited = new LinkedList<String>();

        if (visited.contains(pair)) {
            return true;
        }
        for (String s : visited) {
            newvisited.add(s);
        }
        newvisited.add(pair);
        if (this.currentState().leq(pol.currentState())) {
            for (Id action : allPossibleActions) {
                if (!this.takeTransition(action).leqFSM(
                        pol.takeTransition(action), newvisited)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public RifReaderPolicy takeTransition(Id action) {
        RifReaderPolicy newfsm;
        RifFSMstate nextState = this.current.getNextState(action);
        newfsm = new RifReaderPolicy(this.labelUtil, this.states, nextState);
        return newfsm;
    }

    @Override
    public String toString() {

        StringBuffer sb = new StringBuffer();

        Iterator<Entry<String, RifFSMstate>> it =
                this.states.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, RifFSMstate> stateentry = it.next();
            sb.append(stateentry.getKey());
            if (current == stateentry.getValue()) sb.append("*");
            sb.append(":{");
            List<Principal> principals = stateentry.getValue().principals();
            if (principals != null) {
                Iterator<Principal> ip = principals.iterator();
                while (ip.hasNext()) {
                    Principal p = ip.next();
                    if (!PrincipalUtil.isTopPrincipal(p))
                        sb.append(PrincipalUtil.toString(p));
                    if (ip.hasNext()) {
                        sb.append(",");
                    }
                }
            }
            sb.append("}");
            HashMap<String, RifFSMstate> transitions =
                    stateentry.getValue().getTransitions();
            if (transitions != null) {
                Iterator<Entry<String, RifFSMstate>> itt =
                        transitions.entrySet().iterator();
                while (itt.hasNext()) {
                    Entry<String, RifFSMstate> pairs = itt.next();
                    sb.append(",");
                    sb.append(pairs.getKey());
                    sb.append(":");
                    sb.append(stateentry.getKey());
                    sb.append("->");
                    sb.append(pairs.getValue().name().toString());
                }
            }

            if (it.hasNext()) {
                sb.append(",");
            }
        }
        return sb.toString();

    }

    @Override
    public ConfPolicy join(ConfPolicy p, Set<DelegationPair> s) {
        return join(p, s, true);
    }

    @Override
    public ConfPolicy meet(ConfPolicy p, Set<DelegationPair> s) {
        return meet(p, s, true);
    }

    @Override
    public ConfPolicy join(ConfPolicy p) {
        return join(p, true);
    }

    @Override
    public ConfPolicy meet(ConfPolicy p) {
        return meet(p, true);
    }

    @Override
    public ConfPolicy join(ConfPolicy p, boolean simplify) {
        return labelUtil.join(this, p, simplify);
    }

    @Override
    public ConfPolicy meet(ConfPolicy p, boolean simplify) {
        return labelUtil.meet(this, p, simplify);
    }

    @Override
    public ConfPolicy join(ConfPolicy p, Set<DelegationPair> s, boolean simplify) {
        return labelUtil.join(this, p, s, simplify);
    }

    @Override
    public ConfPolicy meet(ConfPolicy p, Set<DelegationPair> s, boolean simplify) {
        return labelUtil.meet(this, p, s, simplify);
    }
}
