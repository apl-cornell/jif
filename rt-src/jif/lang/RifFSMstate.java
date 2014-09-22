package jif.lang;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class RifFSMstate {

    private String name;
    private List<Principal> principals;
    protected HashMap<String, RifFSMstate> transitions;

    public RifFSMstate(String name, List<Principal> principals,
            HashMap<String, RifFSMstate> transitions) {
        this.name = name;
        this.principals = principals;
        this.transitions = transitions;
        if (this.principals == null) {
            this.principals = new LinkedList<Principal>();
        }
        if (this.transitions == null) {
            this.transitions = new HashMap<String, RifFSMstate>();
        }
    }

    public void setTransition(String transName, RifFSMstate rstate) {
        this.transitions.put(transName, rstate);
    }

    public void addPrincipal(Principal p) {
        if (p != null) this.principals.add(p);
    }

    public boolean hasTopPrincipal() {
        for (Principal p : this.principals) {
            if (PrincipalUtil.isTopPrincipal(p)) return true;
        }
        return false;
    }

    public boolean hasBottomPrincipal() {
        return this.principals.isEmpty();
    }

    public void addPrincipals(Collection<Principal> ps) {
        this.principals.addAll(ps);
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

    public RifFSMstate getNextState(String action) {
        if (this.transitions == null) return this;
        RifFSMstate nextState = this.transitions.get(action);
        if (nextState == null) {
            return this;
        } else {
            return nextState;
        }
    }

    public List<Principal> principals() {
        return this.principals;
    }

    public String name() {
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
        if (this.hasBottomPrincipal() && other.hasBottomPrincipal())
            return true;
        if (this.hasTopPrincipal() && other.hasTopPrincipal()) return true;
        if (this.hasBottomPrincipal() || other.hasBottomPrincipal())
            return false;
        if (this.hasTopPrincipal() || other.hasTopPrincipal()) return false;

        List<Principal> set1 = this.principals;
        List<Principal> set2 = other.principals;
        return set1.containsAll(set2) && set2.containsAll(set1);
    }

    public boolean leq(RifFSMstate other) {
        if (other.hasTopPrincipal()) return true;
        if (this.hasTopPrincipal()) return false;
        if (this.hasBottomPrincipal()) return true;
        if (other.hasBottomPrincipal()) return false;

        List<Principal> set1 = this.principals;
        List<Principal> set2 = other.principals;
        return set1.containsAll(set2);
    }

}
