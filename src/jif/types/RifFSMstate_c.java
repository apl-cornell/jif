package jif.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Id;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;

public class RifFSMstate_c implements RifFSMstate {

    private Id name;
    private List<Principal> principals;
    protected HashMap<String, RifFSMstate> transitions;

    public RifFSMstate_c(RifState s) {
        this.name = s.name();
        this.principals = s.principals();
        this.transitions = new HashMap<String, RifFSMstate>();
    }

    public RifFSMstate_c(Id name, List<Principal> principals,
            HashMap<String, RifFSMstate> transitions) {
        this.name = name;
        this.principals = principals;
        this.transitions = transitions;
    }

    @Override
    public void setTransition(String transName, RifFSMstate rstate) {
        this.transitions.put(transName, rstate);
    }

    @Override
    public HashMap<String, RifFSMstate> getTransitions() {
        return this.transitions;
    }

    @Override
    public RifFSMstate reachedState(String transition) {
        return this.transitions.get(transition);
    }

    @Override
    public RifFSMstate getNextState(Id action) {
        RifFSMstate nextState = this.transitions.get(action.id());
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
    public Id name() {
        return this.name;
    }

    @Override
    public boolean equalsFSM(RifFSMstate other) {
        List<Principal> set1 = this.principals;
        List<Principal> set2 = other.principals();
        return set1.containsAll(set2) && set2.containsAll(set1);
    }

    @Override
    public boolean leqFSM(RifFSMstate other) {
        List<Principal> set1 = this.principals;
        List<Principal> set2 = other.principals();
        return set1.containsAll(set2);
    }

    @Override
    public boolean isCanonical() {
        for (Principal p : this.principals) {
            if (!p.isCanonical()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        for (Principal p : this.principals) {
            if (!p.isRuntimeRepresentable()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        List<Type> throwTypes = new ArrayList<Type>();
        for (Principal p : this.principals) {
            throwTypes.addAll(p.throwTypes(ts));
        }
        return throwTypes;
    }

    @Override
    public boolean isBottomConfidentiality() {
        for (Principal p : this.principals) {
            if (!p.isBottomPrincipal()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isTopConfidentiality() {
        for (Principal p : this.principals) {
            if (!p.isTopPrincipal()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString(boolean current) {
        StringBuffer sb = new StringBuffer(this.name.toString());
        if (current) sb.append("*");
        sb.append(":{");
        Iterator<Principal> ip = this.principals.iterator();
        while (ip.hasNext()) {
            Principal p = ip.next();
            if (!p.isTopPrincipal()) sb.append(p.toString());
            if (ip.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("}");

        Iterator<Entry<String, RifFSMstate>> it =
                this.transitions.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, RifFSMstate> pairs = it.next();
            sb.append(",");
            sb.append(pairs.getKey());
            sb.append(":");
            sb.append(this.name.toString());
            sb.append("->");
            sb.append(pairs.getValue().name().toString());
            it.remove(); // avoids a ConcurrentModificationException
        }
        return sb.toString();
    }

    @Override
    public List<Principal> subst(LabelSubstitution substitution)
            throws SemanticException {
        List<Principal> l = new LinkedList<Principal>();
        boolean changed = false;

        for (Principal p : this.principals) {
            Principal newprincipal = p.subst(substitution);
            if (newprincipal != p) changed = true;
            l.add(newprincipal);
        }

        if (!changed) return null;
        return l;

    }

    @Override
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        // check each principal in turn.
        PathMap X;
        PathMap Xtot = null; //or bottom
        for (Principal p : this.principals) {
            X = p.labelCheck(A, lc);
            A.setPc(X.N(), lc);
            Xtot = Xtot.join(X);
        }
        return Xtot;
    }
}
