package jif.lang;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jif.lang.PrincipalUtil.DelegationPair;
import jif.types.RifFSMstate;
import jif.types.RifFSMstate_c;
import jif.types.principal.Principal;
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

    public RifReaderPolicy addstate(String stateId, String current,
            Set<Principal> principals) {

        HashMap<String, RifFSMstate> transitions =
                new HashMap<String, RifFSMstate>();
        RifFSMstate state =
                new RifFSMstate_c(new Id_c(null, stateId),
                        (List<Principal>) principals, transitions);
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

    @Override
    public boolean relabelsTo(Policy p, Set<DelegationPair> s) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public String toString() {
        return null;
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
