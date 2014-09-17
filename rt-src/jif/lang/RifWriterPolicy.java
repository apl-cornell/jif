package jif.lang;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jif.lang.PrincipalUtil.DelegationPair;
import polyglot.ast.Id;
import polyglot.ast.Id_c;

public class RifWriterPolicy extends AbstractPolicy implements IntegPolicy {

    protected Map<String, RifFSMstate> states;
    protected RifFSMstate current;
    // allPossibleActions contains all the actions that appear in the program.
    // Somehow this list should be initialized when the whole program is parsed.
    private LinkedList<Id> allPossibleActions;

    public RifWriterPolicy(LabelUtil labelUtil) {
        super(labelUtil);
        states = new HashMap<String, RifFSMstate>();
        allPossibleActions = new LinkedList<Id>();
        int i;
        for (i = 0; i < 5; i++) {
            allPossibleActions.add(new Id_c(null, "f" + Integer.toString(i)));
        }
    }

    public RifWriterPolicy(LabelUtil labelUtil,
            Map<String, RifFSMstate> states, RifFSMstate current) {
        super(labelUtil);
        this.states = states;
        this.current = current;

        allPossibleActions = new LinkedList<Id>();
        int i;
        for (i = 0; i < 5; i++) {
            allPossibleActions.add(new Id_c(null, "f" + Integer.toString(i)));
        }
    }

    public RifWriterPolicy addstate(String stateId, String current,
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

    public RifWriterPolicy addprincipal(String stateId, Principal p) {

        for (Entry<String, RifFSMstate> st : states.entrySet()) {
            if (st.getKey() == stateId) {
                st.getValue().addPrincipal(p);
            }
        }
        return this;

    }

    public RifWriterPolicy addtransition(String transition, String state1,
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

    public Map<String, RifFSMstate> states() {
        return this.states;
    }

    public RifWriterPolicy times(RifWriterPolicy p) {
        if (p == null) {
            return this;
        }

        HashMap<String, RifFSMstate> states =
                new HashMap<String, RifFSMstate>();
        RifFSMstate newcurrentstate = null;
        RifWriterPolicy fsm1 = this;
        RifWriterPolicy fsm2 = p;

        LinkedList<Id> allPossibleActions = new LinkedList<Id>();
        int j;
        for (j = 0; j < 5; j++) {
            allPossibleActions.add(new Id_c(null, "f" + Integer.toString(j)));
        }

        Iterator<Entry<String, RifFSMstate>> it1 =
                fsm1.states().entrySet().iterator();
        while (it1.hasNext()) {
            Entry<String, RifFSMstate> pairs1 = it1.next();
            Iterator<Entry<String, RifFSMstate>> it2 =
                    fsm2.states().entrySet().iterator();
            while (it2.hasNext()) {
                Entry<String, RifFSMstate> pairs2 = it2.next();
                String newname = pairs1.getKey() + "&" + pairs2.getKey();
                List<Principal> newprincipals = new LinkedList<Principal>();
                for (Principal princ : pairs1.getValue().principals()) {
                    if (pairs2.getValue().principals().contains(princ)) {
                        newprincipals.add(princ);
                    }
                }
                states.put(newname, new RifFSMstate(new Id_c(null, newname),
                        newprincipals, new HashMap<String, RifFSMstate>()));
                if (fsm1.currentState().name().id() == pairs1.getKey()
                        && fsm2.currentState().name().id() == pairs2.getKey())
                    newcurrentstate = states.get(newname);
            }
        }

        it1 = fsm1.states().entrySet().iterator();
        while (it1.hasNext()) {
            Entry<String, RifFSMstate> pairs1 = it1.next();
            Iterator<Entry<String, RifFSMstate>> it2 =
                    fsm2.states().entrySet().iterator();
            while (it2.hasNext()) {
                Entry<String, RifFSMstate> pairs2 = it2.next();
                RifFSMstate currentstate =
                        states.get(pairs1.getKey() + "&" + pairs2.getKey());
                for (Id action : allPossibleActions) {
                    RifFSMstate reachedstate1 =
                            pairs1.getValue().reachedState(action.id());
                    RifFSMstate reachedstate2 =
                            pairs2.getValue().reachedState(action.id());
                    String reachedname =
                            reachedstate1.name().id() + "&"
                                    + reachedstate2.name().id();
                    currentstate.setTransition(action.id(),
                            states.get(reachedname));
                }
            }
        }

        return new RifWriterPolicy(this.labelUtil, states, newcurrentstate);
    }

    @Override
    public boolean relabelsTo(Policy p, Set<DelegationPair> s) {
        Set<Policy> newlist = new LinkedHashSet<Policy>();
        if (this == p || this.equals(p)) return true;

        if (p instanceof JoinIntegPolicy) {
            JoinPolicy jp = (JoinPolicy) p;
            RifWriterPolicy res = null;
            for (Policy pi : jp.joinComponents()) {
                if (pi instanceof RifWriterPolicy) {
                    RifWriterPolicy pol = (RifWriterPolicy) pi;
                    res = pol.times(res);
                } else {
                    newlist.add(pi);
                }
            }
            if (res != null) {
                newlist.add(res);
            }
            for (Policy pi : newlist) {
                if (labelUtil.relabelsTo(this, pi, s)) return true;
            }
            return false;
        } else if (p instanceof MeetIntegPolicy) {
            return false; //do we need to fill it???
        } else if (!(p instanceof WriterPolicy)) return false;

        RifWriterPolicy pp = (RifWriterPolicy) p;

        return leqFSM(pp, new LinkedList<String>());
    }

    @Override
    public int hashCode() {
        return 5555678;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof RifWriterPolicy)) {
            return false;
        }

        RifWriterPolicy policy = (RifWriterPolicy) o;

        return equalsFSM(policy, new LinkedList<String>());
    }

    public boolean equalsFSM(RifWriterPolicy pol, List<String> visited) {
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

    public boolean leqFSM(RifWriterPolicy pol, List<String> visited) {
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
        if (pol.currentState().leq(this.currentState())) {
            for (Id action : allPossibleActions) {
                if (!pol.takeTransition(action).leqFSM(
                        this.takeTransition(action), newvisited)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public RifWriterPolicy takeTransition(Id action) {
        RifWriterPolicy newfsm;
        RifFSMstate nextState = this.current.getNextState(action.id());
        newfsm = new RifWriterPolicy(this.labelUtil, this.states, nextState);
        return newfsm;
    }

    public RifWriterPolicy takeTransition(String action) {
        RifWriterPolicy newfsm;
        RifFSMstate nextState = this.current.getNextState(action);
        newfsm = new RifWriterPolicy(this.labelUtil, this.states, nextState);
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
    public IntegPolicy join(IntegPolicy p, Set<DelegationPair> s) {
        return join(p, s, true);
    }

    @Override
    public IntegPolicy meet(IntegPolicy p, Set<DelegationPair> s) {
        return meet(p, s, true);
    }

    @Override
    public IntegPolicy join(IntegPolicy p) {
        return join(p, true);
    }

    @Override
    public IntegPolicy meet(IntegPolicy p) {
        return meet(p, true);
    }

    @Override
    public IntegPolicy join(IntegPolicy p, boolean simplify) {
        return labelUtil.join(this, p, simplify);
    }

    @Override
    public IntegPolicy meet(IntegPolicy p, boolean simplify) {
        return labelUtil.meet(this, p, simplify);
    }

    @Override
    public IntegPolicy join(IntegPolicy p, Set<DelegationPair> s,
            boolean simplify) {
        return labelUtil.join(this, p, s, simplify);
    }

    @Override
    public IntegPolicy meet(IntegPolicy p, Set<DelegationPair> s,
            boolean simplify) {
        return labelUtil.meet(this, p, s, simplify);
    }

}