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
import polyglot.util.SerialVersionUID;

public class RifFSMstate_c implements RifFSMstate {
    private static final long serialVersionUID = SerialVersionUID.generate();

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
        if (this.transitions == null) return this;
        RifFSMstate nextState = this.transitions.get(transition);
        if (nextState == null) {
            return this;
        } else {
            return nextState;
        }
    }

    @Override
    public RifFSMstate getNextState(Id action) {
        if (this.transitions == null) return this;
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
    public List<Principal> confEquivPrincipals() {
        List<Principal> l = new LinkedList<Principal>();
        if (this.principals == null) return null;

        for (Principal p : this.principals) {
            if (p.isBottomPrincipal()) {
                List<Principal> t = new LinkedList<Principal>();
                t.add(p);
                return t;
            }
            if (!p.isTopPrincipal()) l.add(p);
        }
        return l;
    }

    @Override
    public boolean equalsFSM(RifFSMstate other) {
        List<Principal> set1 = this.confEquivPrincipals();
        List<Principal> set2 = other.confEquivPrincipals();

        if (set1 == null && set2 == null) return true;
        if (set1 == null || set2 == null) return false;
        return set1.containsAll(set2) && set2.containsAll(set1);
    }

    @Override
    public boolean leqFSM(RifFSMstate other) {
        List<Principal> set1 = this.confEquivPrincipals();
        List<Principal> set2 = other.confEquivPrincipals();
        if (set2 == null) return true;
        if (set1 == null) return false;
        if (set1.size() == 1 && set1.get(0).isBottomPrincipal()) return true;
        return set1.containsAll(set2);
    }

    @Override
    public boolean isCanonical() {
        if (this.principals == null) return true;
        for (Principal p : this.principals) {
            if (!p.isCanonical()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        if (this.principals == null) return true;
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
        if (this.principals == null)
            throwTypes = null;
        else {
            for (Principal p : this.principals) {
                throwTypes.addAll(p.throwTypes(ts));
            }
        }
        return throwTypes;
    }

    @Override
    public boolean isBottomConfidentiality() {
        List<Principal> l = this.confEquivPrincipals();
        if (l == null) return false;
        return l.size() == 1 && l.get(0).isBottomPrincipal();
    }

    @Override
    public boolean isTopConfidentiality() {
        return this.confEquivPrincipals() == null;
    }

    @Override
    public String toString(boolean current) {
        StringBuffer sb = new StringBuffer(this.name.toString());
        if (current) sb.append("*");
        sb.append(":{");
        if (this.principals != null) {
            Iterator<Principal> ip = this.principals.iterator();
            while (ip.hasNext()) {
                Principal p = ip.next();
                if (!p.isTopPrincipal()) sb.append(p.toString());
                if (ip.hasNext()) {
                    sb.append(",");
                }
            }
        }
        sb.append("}");

        if (this.transitions != null) {
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
            }
        }
        return sb.toString();
    }

    @Override
    public List<Principal> subst(LabelSubstitution substitution)
            throws SemanticException {
        List<Principal> l = new LinkedList<Principal>();
        boolean changed = false;

        if (this.principals == null) return null;
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
        if (this.principals == null) return null;
        for (Principal p : this.principals) {
            X = p.labelCheck(A, lc);
            A.setPc(X.N(), lc);
            Xtot = Xtot.join(X);
        }
        return Xtot;
    }
}
