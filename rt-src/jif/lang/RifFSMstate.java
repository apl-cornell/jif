package jif.lang;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import polyglot.ast.Id;

public class RifFSMstate {

    private Id name;
    private List<Principal> principals;
    protected HashMap<String, RifFSMstate> transitions;

    public RifFSMstate(Id name, List<Principal> principals,
            HashMap<String, RifFSMstate> transitions) {
        this.name = name;
        this.principals = principals;
        this.transitions = transitions;
    }

    public void setTransition(String transName, RifFSMstate rstate) {
        this.transitions.put(transName, rstate);
    }

    public HashMap<String, RifFSMstate> getTransitions() {
        return this.transitions;
    }

    public RifFSMstate reachedState(String transition) {
        if (this.transitions == null) return this;
        RifFSMstate nextState = this.transitions.get(transition);
        if (nextState == null) {
            return this;
        } else {
            return nextState;
        }
    }

    public RifFSMstate getNextState(Id action) {
        if (this.transitions == null) return this;
        RifFSMstate nextState = this.transitions.get(action.id());
        if (nextState == null) {
            return this;
        } else {
            return nextState;
        }
    }

    public List<Principal> principals() {
        return this.principals;
    }

    public Id name() {
        return this.name;
    }

    public List<Principal> confEquivPrincipals() {
        List<Principal> l = new LinkedList<Principal>();
        if (this.principals == null || this.principals.isEmpty()) return null;

        for (Principal p : this.principals) {
            // if (p.isBottomPrincipal()) {
            //     List<Principal> t = new LinkedList<Principal>();
            //     t.add(p);
            //     return t;
            // } Is it correct?
            if (!PrincipalUtil.isTopPrincipal(p)) l.add(p);
        }
        return l;
    }

    public boolean equals(RifFSMstate other) {
        List<Principal> set1 = this.confEquivPrincipals();
        List<Principal> set2 = other.confEquivPrincipals();

        if (set1 == null && set2 == null) return true;
        if (set1 == null || set2 == null) return false;
        return set1.containsAll(set2) && set2.containsAll(set1);
    }

    public boolean leq(RifFSMstate other) {
        List<Principal> set1 = this.confEquivPrincipals();
        List<Principal> set2 = other.confEquivPrincipals();
        if (set2 == null) return true;
        if (set1 == null) return false;
        // if (set1.size() == 1 && set1.get(0).isBottomPrincipal()) return true; //is it correct?
        return set1.containsAll(set2);
    }

}
