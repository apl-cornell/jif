package jif.types.hierarchy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jif.types.principal.ConjunctivePrincipal;
import jif.types.principal.DisjunctivePrincipal;
import jif.types.principal.Principal;

/** The principal hierarchy that defines the acts-for relationships
 *  between principals.
 */
public class PrincipalHierarchy {
    /**
     * Map from Principal to Set[Principal], where if p actsfor p', then
     * p' is in the set actsfor.get(p)
     */
    private final Map<Principal, Set<Principal>> actsfor;

    /**
     * Map from Principal to Set[Principal], where if p' actsfor p, then
     * p' is in the set actsfor.get(p)
     */
    private final Map<Principal, Set<Principal>> grants;

    /**
     * Cache of results, same domain and range as actsfor.
     */
    private final Map<Principal, Set<Principal>> actorCache;

    public PrincipalHierarchy() {
        this.actsfor = new HashMap<Principal, Set<Principal>>();
        this.grants = new HashMap<Principal, Set<Principal>>();
        this.actorCache = new HashMap<Principal, Set<Principal>>();
    }

    @Override
    public String toString() {
        return "[" + actsForString() + "]";
    }

    private static void addAlreadyReported(
            Map<Principal, Set<Principal>> alreadyReported, Principal p,
            Principal q) {
        // record the fact that we have already reported that q actsfor p
        Set<Principal> s = alreadyReported.get(q);
        if (s == null) {
            s = new HashSet<Principal>();
            alreadyReported.put(q, s);
        }
        s.add(p);
    }

    private static boolean isAlreadyReported(
            Map<Principal, Set<Principal>> alreadyReported, Principal p,
            Principal q) {
        Set<Principal> s = alreadyReported.get(p);
        if (s != null) {
            return s.contains(q);
        }
        return false;
    }

    public String actsForString() {
        StringBuffer sb = new StringBuffer();
        Map<Principal, Set<Principal>> alreadyReported =
                new HashMap<Principal, Set<Principal>>();
        boolean needsComma = false;
        for (Map.Entry<Principal, Set<Principal>> e : actsfor.entrySet()) {
            Principal p = e.getKey();
            Set<Principal> a = e.getValue();

            for (Principal q : a) {
                if (isAlreadyReported(alreadyReported, p, q)) {
                    continue;
                }
                if (needsComma) {
                    sb.append(", ");
                }
                sb.append("(");
                sb.append(p.toString());
                Set<Principal> b = actsfor.get(q);
                if (b != null && b.contains(p)) {
                    // q also acts for p
                    sb.append(" equiv ");
                    addAlreadyReported(alreadyReported, p, q);
                } else {
                    sb.append(" actsFor ");
                }
                sb.append(q.toString());
                sb.append(")");
                needsComma = true;
            }
        }
        return sb.toString();
    }

    public boolean isEmpty() {
        return actsfor.isEmpty();
    }

    public void add(Principal actor, Principal granter) {
        Set<Principal> s = actsfor.get(actor);
        if (s == null) {
            // create a new set of granting principals
            s = new LinkedHashSet<Principal>();
            actsfor.put(actor, s);
        }
        s.add(granter);

        Set<Principal> t = grants.get(granter);
        if (t == null) {
            // create a new set of granting principals
            t = new LinkedHashSet<Principal>();
            grants.put(granter, t);
        }
        t.add(actor);
    }

    public boolean actsFor(Principal actor, Principal granter) {
        return actsFor(actor, granter, new LinkedList<PrincipalPair>());
    }

    private static class PrincipalPair {
        PrincipalPair(Principal actor, Principal granter) {
            this.actor = actor;
            this.granter = granter;
        }

        final Principal actor;
        final Principal granter;

        @Override
        public int hashCode() {
            return actor.hashCode() ^ granter.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof PrincipalPair) {
                PrincipalPair that = (PrincipalPair) o;
                return this.actor.equals(that.actor)
                        && this.granter.equals(that.granter);
            }
            return false;
        }
    }

    protected boolean actsFor(Principal actor, Principal granter,
            LinkedList<PrincipalPair> goalStack) {
        if (actor.isTopPrincipal()) return true;
        if (granter.isBottomPrincipal()) return true;

        Set<Principal> actorCached = actorCache.get(actor);
        if (actorCached != null && actorCached.contains(granter)) return true;

        PrincipalPair currentGoal = new PrincipalPair(actor, granter);
        if (goalStack.contains(currentGoal)) {
            // this goal is already on the stack.
            return false;
        }

        // Check the reflexive part of actsFor relation.
        if (actor.equals(granter)) {
            return true;
        }

        Set<Principal> s = actsfor.get(actor);
        if (s != null && s.contains(granter)) {
            // explicit actsfor in the hierarchy
            cacheResult(actor, granter);
            return true;
        }

        // push this goal on the stack before making recursive calls
        goalStack.addLast(currentGoal);

        // special cases for conjunctive and disjunctive principals.
        if (actor instanceof ConjunctivePrincipal) {
            ConjunctivePrincipal cp = (ConjunctivePrincipal) actor;
            // cp actsfor granter if at least one of the conjucts act for granter
            for (Principal p : cp.conjuncts()) {
                if (actsFor(p, granter, goalStack)) {
                    cacheResult(actor, granter);
                    return true;
                }
            }
        }
        if (actor instanceof DisjunctivePrincipal) {
            DisjunctivePrincipal dp = (DisjunctivePrincipal) actor;
            // dp actsfor granter if all of the disjucts act for granter
            boolean all = true;
            for (Principal p : dp.disjuncts()) {
                if (!actsFor(p, granter, goalStack)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                cacheResult(actor, granter);
                return true;
            }
        }

        if (granter instanceof DisjunctivePrincipal) {
            DisjunctivePrincipal dp = (DisjunctivePrincipal) granter;
            // actor actsfor dp if there is one disjunct that actor can act for
            for (Principal p : dp.disjuncts()) {
                if (actsFor(actor, p, goalStack)) {
                    cacheResult(actor, granter);
                    return true;
                }
            }
        }

        if (granter instanceof ConjunctivePrincipal) {
            ConjunctivePrincipal cp = (ConjunctivePrincipal) granter;
            // actor actsfor cp if actor actsfor all conjuncts
            boolean all = true;
            for (Principal p : cp.conjuncts()) {
                if (!actsFor(actor, p, goalStack)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                cacheResult(actor, granter);
                return true;
            }

        }

        // Check the transitive part of actsFor relation.
        if (s != null) {
            for (Principal p : s) {
                if (actsFor(p, granter, goalStack)) {
                    cacheResult(actor, granter);
                    return true;
                }
            }
        }

        // now also go through the grants set
        Set<Principal> t = grants.get(granter);
        if (t != null) {
            for (Principal p : t) {
                if (actsFor(actor, p, goalStack)) {
                    cacheResult(actor, granter);
                    return true;
                }
            }
        }

        // we've failed, remove the current goal from the stack.
        goalStack.removeLast();
        return false;
    }

    private void cacheResult(Principal actor, Principal granter) {
        Set<Principal> s = actorCache.get(actor);
        if (s == null) {
            s = new HashSet<Principal>();
            actorCache.put(actor, s);
        }
        s.add(granter);
    }

    public PrincipalHierarchy copy() {
        PrincipalHierarchy dup = new PrincipalHierarchy();

        for (Entry<Principal, Set<Principal>> e : actsfor.entrySet()) {
            Principal p = e.getKey();
            Set<Principal> s = e.getValue();
            dup.actsfor.put(p, new LinkedHashSet<Principal>(s));
        }
        for (Entry<Principal, Set<Principal>> e : grants.entrySet()) {
            Principal p = e.getKey();
            Set<Principal> s = e.getValue();
            dup.grants.put(p, new LinkedHashSet<Principal>(s));
        }

        return dup;
    }

    public void clear() {
        actsfor.clear();
    }
}
