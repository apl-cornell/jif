package jif.lang;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jif.lang.PrincipalUtil.DelegationPair;
import jif.types.RifComponent;
import jif.types.RifFSMstate;
import jif.types.RifFSMstate_c;
import jif.types.RifState;
import jif.types.RifTransition;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.ast.Id;
import polyglot.ast.Id_c;

public class RifReaderPolicy extends AbstractPolicy implements ConfPolicy {

    protected Map<String, RifFSMstate> states;
    protected RifFSMstate current;
    // allPossibleActions contains all the actions that appear in the program.
    // Somehow this list should be initialized when the whole program is parsed.
    private LinkedList<Id> allPossibleActions;

    public RifReaderPolicy(LabelUtil labelUtil, String str) {
        super(labelUtil);

        String currName = null;
        Map<String, RifFSMstate> states = new HashMap<String, RifFSMstate>();
        List<RifState> ls = new LinkedList<RifState>();
        List<RifTransition> lt = new LinkedList<RifTransition>();

        String delims = "[,]";
        String[] tokens = str.split(delims);
        int i = 0;

        while (i < tokens.length) {
            if (tokens[i] == "STB") {
                List<Principal> principals = new LinkedList<Principal>();
                i++;
                while (tokens[i] != "STE") {
                    principals.add((Expr) tokens[i]);
                }
            }
        }

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

    public Principal owner() {
        return owner;
    }

    public Principal reader() {
        return reader;
    }

    @Override
    public boolean relabelsTo(Policy p, Set<DelegationPair> s) {
        if (this == p || this.equals(p)) return true;

        if (p instanceof JoinConfPolicy) {
            JoinPolicy jp = (JoinPolicy) p;
            // this <= p1 join ... join p2 if there exists a pi such that
            // this <= pi
            for (Policy pi : jp.joinComponents()) {
                if (labelUtil.relabelsTo(this, pi, s)) return true;
            }
            return false;
        } else if (p instanceof MeetConfPolicy) {
            MeetPolicy mp = (MeetPolicy) p;
            // this <= p1 meet ... meet p2 if for all pi
            // this <= pi
            Set<DelegationPair> temp = new HashSet<DelegationPair>();
            for (Policy pi : mp.meetComponents()) {
                if (!labelUtil.relabelsTo(this, pi, temp)) return false;
            }
            s.addAll(temp);
            return true;
        } else if (!(p instanceof ReaderPolicy)) return false;

        ReaderPolicy pp = (ReaderPolicy) p;

        // this = { o  : .. ri  .. }
        // p    = { o' : .. rj' .. }

        // o' >= o?

        ActsForProof ownersProof = PrincipalUtil.actsForProof(pp.owner, owner);
        if (ownersProof == null) {
            return false;
        }
        ActsForProof readerReaderProof =
                PrincipalUtil.actsForProof(pp.reader, this.reader);
        if (readerReaderProof != null) {
            ownersProof.gatherDelegationDependencies(s);
            readerReaderProof.gatherDelegationDependencies(s);
            return true;
        }
        ActsForProof readerOwnerProof =
                PrincipalUtil.actsForProof(pp.reader, this.owner);
        if (readerOwnerProof != null) {
            ownersProof.gatherDelegationDependencies(s);
            readerOwnerProof.gatherDelegationDependencies(s);
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (owner == null ? 0 : owner.hashCode())
                ^ (reader == null ? 0 : reader.hashCode()) ^ 4238;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReaderPolicy)) {
            return false;
        }

        ReaderPolicy policy = (ReaderPolicy) o;

        if (owner == policy.owner
                || (owner != null && owner.equals(policy.owner)
                        && policy.owner != null && policy.owner.equals(owner))) {
            return (reader == policy.reader || (reader != null
                    && reader.equals(policy.reader) && policy.reader != null && policy.reader
                        .equals(reader)));
        }

        return false;
    }

    @Override
    public String toString() {
        String str = PrincipalUtil.toString(owner) + "->";
        if (!PrincipalUtil.isTopPrincipal(reader))
            str += PrincipalUtil.toString(reader);
        return str;
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
