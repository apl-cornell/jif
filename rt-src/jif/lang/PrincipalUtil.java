package jif.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility methods for principals.
 * 
 * See the Jif source code, in lib-src/jif/lang/PrincipalUtil.jif
 */
public class PrincipalUtil {
    private static Principal TOP_PRINCIPAL = new TopPrincipal();

    // caches
    private static final Map<ActsForPair, ActsForProof> cacheActsFor =
            new ConcurrentHashMap<ActsForPair, ActsForProof>();
    private static final Map<ActsForPair, ActsForPair> cacheNotActsFor =
            new ConcurrentHashMap<ActsForPair, ActsForPair>(); // effectively a set

    //  map from DelegationPairs to sets of ActsForPairs. If (p, q) is
    // in the set of the map of delegation d, then p actsfor q, and the
    // proof depends on the delegation d
    private static final Map<DelegationPair, Set<ActsForPair>> cacheActsForDependencies =
            new ConcurrentHashMap<DelegationPair, Set<ActsForPair>>();

    /**
     * Returns true if and only if the principal p acts for the principal q. A
     * synonym for the <code>actsFor</code> method.
     */
    public static boolean acts_for(Principal p, Principal q) {
        try {
            LabelUtil.singleton().enterTiming();
            return actsFor(p, q);
        } finally {
            LabelUtil.singleton().exitTiming();
        }
    }

    /**
     * Returns true if and only if the principal p acts for the principal q.
     */
    public static boolean actsFor(Principal p, Principal q) {
        return actsForProof(p, q) != null;
    }

    /**
     * Returns an actsfor proof if and only if the principal p acts for the principal q.
     */
    public static ActsForProof actsForProof(Principal p, Principal q) {
        try {
            LabelUtil.singleton().enterTiming();
            // try cache
            ActsForPair pair = new ActsForPair(p, q);
            if (LabelUtil.USE_CACHING) {
                if (cacheActsFor.containsKey(pair)) {
                    return cacheActsFor.get(pair);
                }
                if (cacheNotActsFor.containsKey(pair)) return null;
            }

            if (delegatesTo(q, p)) return new DelegatesProof(p, q);

            // if the two principals are ==-equal, or if they
            // both agree that they are equal to each other, then
            // we return true (since the acts-for relation is
            // reflexive).
            if (eq(p, q)) return new ReflexiveProof(p, q);

            // try searching
            ActsForProof prf = findActsForProof(p, q, null);
            if (prf != null && (verifyProof(prf, p, q))) {
                if (LabelUtil.USE_CACHING) {
                    cacheActsFor.put(pair, prf);
                }
                // add dependencies that this actsfor replies on.
                Set<DelegationPair> s = new HashSet<DelegationPair>();
                prf.gatherDelegationDependencies(s);
                // for each DelegationPair in s, if that delegation is removed, the proof is no longer valid.
                if (LabelUtil.USE_CACHING) {
                    for (DelegationPair del : s) {
                        Set<ActsForPair> deps =
                                cacheActsForDependencies.get(del);
                        if (deps == null) {
                            deps = new HashSet<ActsForPair>();
                            cacheActsForDependencies.put(del, deps);
                        }
                        deps.add(pair);
                    }
                }
                return prf;
            }

            if (LabelUtil.USE_CACHING) {
                cacheNotActsFor.put(pair, pair);
            }
            return null;
        } finally {
            LabelUtil.singleton().exitTiming();
        }
    }

    /**
     * Notification that a new delegation has been created.
     */
    public static void notifyNewDelegation(Principal granter,
            Principal superior) {
        // double check that the delegation occured
        if (!delegatesTo(granter, superior)) return;

        // XXX for the moment, just clear out all cached negative results
        if (LabelUtil.USE_CACHING) {
            cacheNotActsFor.clear();
        }

        // need to notify the label cache too
        LabelUtil.singleton().notifyNewDelegation(granter, superior);
    }

    /**
     * Notification that an existing delegation has been revoked.
     */
    public static void notifyRevokeDelegation(Principal granter,
            Principal superior) {
        if (LabelUtil.USE_CACHING) {
            DelegationPair del = new DelegationPair(superior, granter);
            Set<ActsForPair> deps = cacheActsForDependencies.remove(del);
            if (deps != null) {
                for (ActsForPair afp : deps) {
                    cacheActsFor.remove(afp);
                }
            }
        }
        // need to notify the label cache too
        LabelUtil.singleton().notifyRevokeDelegation(granter, superior);
    }

    /**
     * Search for an ActsForProof between p and q. An ActsForProof between
     * p and q is a a checkable proof object.
     * @param p
     * @param q
     * @param searchState records the goals that we are in the middle of attempting
     * @return An ActsForProof between p and q, or null if none can be found.
     */
    public static ActsForProof findActsForProof(Principal p, Principal q,
            Object searchState) {
        try {
            LabelUtil.singleton().enterTiming();
            // try the dumb things first.
            if (q == null) {
                return new DelegatesProof(p, q);
            }
            if (eq(p, q)) {
                return new ReflexiveProof(p, q);
            }

            // check the search state
            ProofSearchState newss;
            if (searchState instanceof ProofSearchState) {
                ProofSearchState ss = (ProofSearchState) searchState;
                if (ss.contains(p, q)) {
                    // p and q are already on the goal stack. Prevent an infinite recursion.
                    return null;
                }
                newss = new ProofSearchState(ss, p, q);
            } else {
                newss = new ProofSearchState(p, q);
            }

            // if we're going from a dis/conjunctive principal, try finding a downwards
            // proof first
            ActsForProof prf;
            boolean doneDownTo = false;
            if (p instanceof ConjunctivePrincipal
                    || p instanceof DisjunctivePrincipal) {
                prf = p.findProofDownto(q, newss);
                if (prf != null) return prf;
                doneDownTo = true;
            }

            // try searching upwards from q.
            prf = q.findProofUpto(p, newss);
            if (prf != null) return prf;

            // try searching downwards from p.
            if (!doneDownTo && p != null) {
                prf = p.findProofDownto(q, newss);
                if (prf != null) return prf;
            }

            // have failed!
            return null;
        } finally {
            LabelUtil.singleton().exitTiming();
        }

    }

    private static class ProofSearchState {
        private ActsForPair[] goalstack;

        public ProofSearchState(Principal p, Principal q) {
            goalstack = new ActsForPair[1];
            goalstack[0] = new ActsForPair(p, q);
        }

        private ProofSearchState(ProofSearchState ss, Principal p,
                Principal q) {
            int len = ss.goalstack.length + 1;
            goalstack = new ActsForPair[len];
            System.arraycopy(ss.goalstack, 0, goalstack, 0, len - 1);
            goalstack[len - 1] = new ActsForPair(p, q);
        }

        public boolean contains(Principal p, Principal q) {
            for (ActsForPair element : goalstack) {
                if (element != null) {
                    if (eq(element.p, p) && eq(element.q, q)) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

    /**
     * Return whether principals p and q are equal. p and q must either be references to the same object,
     * both be null, or agree that they are equal to the other.
     */
    private static boolean eq(Principal p, Principal q) {
        return p == q || (p != null && q != null && p.equals(q) && q.equals(p));
    }

    /**
     * Verify that the chain is a valid delegates-chain between p and q. That
     * is, q == chain[n], chain[n] delegates to chain[n-1], ..., chain[0] == p,
     * i.e., p acts for q.
     * 
     */
    public static boolean verifyProof(ActsForProof prf, Principal actor,
            Principal granter) {
        try {
            LabelUtil.singleton().enterTiming();
            if (prf == null) return false;
            if (prf.getActor() != actor || prf.getGranter() != granter)
                return false;

            if (prf instanceof DelegatesProof) {
                return delegatesTo(granter, actor);
            } else if (prf instanceof ReflexiveProof) {
                return eq(actor, granter);
            } else if (prf instanceof TransitiveProof) {
                TransitiveProof proof = (TransitiveProof) prf;
                return verifyProof(proof.getActorToP(), proof.getActor(),
                        proof.getP())
                        && verifyProof(proof.getPToGranter(), proof.getP(),
                                proof.getGranter());
            } else if (prf instanceof FromDisjunctProof) {
                FromDisjunctProof proof = (FromDisjunctProof) prf;
                if (actor instanceof DisjunctivePrincipal) {
                    DisjunctivePrincipal dp = (DisjunctivePrincipal) actor;
                    // go though each disjunct, and make sure there is a proof
                    // from the disjunct to the granter
                    for (Principal disjunct : dp.disjuncts) {
                        ActsForProof pr =
                                proof.getDisjunctProofs().get(disjunct);
                        if (!verifyProof(pr, disjunct, granter)) return false;
                    }
                    // we have verified a proof from each disjunct to the granter
                    return true;
                }

            } else if (prf instanceof ToConjunctProof) {
                ToConjunctProof proof = (ToConjunctProof) prf;
                if (granter instanceof ConjunctivePrincipal) {
                    ConjunctivePrincipal cp = (ConjunctivePrincipal) granter;
                    // go though each conjunct, and make sure there is a proof
                    // from actor to the conjunct
                    for (Principal conjunct : cp.conjuncts) {
                        ActsForProof pr =
                                proof.getConjunctProofs().get(conjunct);
                        if (!verifyProof(pr, actor, conjunct)) return false;
                    }
                    // we have verified a proof from actor to each conjunct.
                    return true;
                }

            }

            // unknown proof!
            return false;
        } finally {
            LabelUtil.singleton().exitTiming();
        }

    }

    public static boolean delegatesTo(Principal granter, Principal superior) {
        try {
            LabelUtil.singleton().enterTiming();
            if (granter == null) return true;
            if (topPrincipal().equals(superior)) return true;
            if (superior instanceof ConjunctivePrincipal) {
                ConjunctivePrincipal cp = (ConjunctivePrincipal) superior;
                for (Principal conjunct : cp.conjuncts) {
                    if (equals(conjunct, granter)) return true;
                }
            }
            return granter.delegatesTo(superior);
        } finally {
            LabelUtil.singleton().exitTiming();
        }

    }

    public static boolean equivalentTo(Principal p, Principal q) {
        try {
            LabelUtil.singleton().enterTiming();
            return actsFor(p, q) && actsFor(q, p);
        } finally {
            LabelUtil.singleton().exitTiming();
        }

    }

    public static boolean equals(Principal p, Principal q) {
        try {
            LabelUtil.singleton().enterTiming();
            return eq(p, q);
        } finally {
            LabelUtil.singleton().exitTiming();
        }

    }

    /**
     * Execute the given closure, if the principal agrees.
     */
    public static Object execute(Principal p, Object authPrf, Closure c,
            Label lb) {
        Capability cap = authorize(p, authPrf, c, lb, true);
        if (cap != null) {
            return cap.invoke();
        }
        return null;
    }

    /**
     * Obtain a Capability for the given principal and closure.
     */
    public static Capability authorize(Principal p, Object authPrf, Closure c,
            Label lb) {
        return authorize(p, authPrf, c, lb, false);
    }

    private static Capability authorize(Principal p, Object authPrf, Closure c,
            Label lb, boolean executeNow) {
        try {
            LabelUtil.singleton().enterTiming();
            Principal closureP = c.jif$getjif_lang_Closure_P();
            Label closureL = c.jif$getjif_lang_Closure_L();
            if (closureP == p || (p != null && closureP != null
                    && p.equals(closureP) && closureP.equals(p))) {
                // The principals agree.
                if (LabelUtil.singleton().equivalentTo(closureL, lb)) {
                    // the labels agree
                    if (p == null
                            || p.isAuthorized(authPrf, c, lb, executeNow)) {
                        // either p is null (and the "null" principal always
                        // gives authority!) or p grants authority to execute the
                        // closure.
                        return new Capability(closureP, closureL, c);
                    }
                }
            }
            return null;
        } finally {
            LabelUtil.singleton().exitTiming();
        }

    }

    /**
     * returns the null principal, the principal that every other principal can
     * act for.
     */
    public static Principal nullPrincipal() {
        return null;
    }

    public static Principal bottomPrincipal() {
        try {
            LabelUtil.singleton().enterTiming();
            return nullPrincipal();
        } finally {
            LabelUtil.singleton().exitTiming();
        }

    }

    public static Principal topPrincipal() {
        return TOP_PRINCIPAL;
    }

    static boolean isTopPrincipal(Principal p) {
        return p == TOP_PRINCIPAL;
    }

    public static ConfPolicy readableByPrinPolicy(Principal p) {
        try {
            LabelUtil.singleton().enterTiming();
            return new ReaderPolicy(LabelUtil.singleton(), topPrincipal(), p);
        } finally {
            LabelUtil.singleton().exitTiming();
        }

    }

    public static IntegPolicy writableByPrinPolicy(Principal p) {
        try {
            LabelUtil.singleton().enterTiming();
            return new WriterPolicy(LabelUtil.singleton(), topPrincipal(), p);
        } finally {
            LabelUtil.singleton().exitTiming();
        }
    }

    public static Principal disjunction(Principal left, Principal right) {
        try {
            LabelUtil.singleton().enterTiming();
            if (left == null || right == null) return null;
            if (actsFor(left, right)) return right;
            if (actsFor(right, left)) return left;
            Collection<Principal> c = new ArrayList<Principal>(2);
            c.add(left);
            c.add(right);
            return disjunction(c);
        } finally {
            LabelUtil.singleton().exitTiming();
        }

    }

    public static Principal conjunction(Principal left, Principal right) {
        try {
            LabelUtil.singleton().enterTiming();
            if (left == null) return right;
            if (right == null) return left;
            if (actsFor(left, right)) return left;
            if (actsFor(right, left)) return right;
            Collection<Principal> c = new ArrayList<Principal>(2);
            c.add(left);
            c.add(right);
            return conjunction(c);
        } finally {
            LabelUtil.singleton().exitTiming();
        }

    }

    public static Principal disjunction(Collection<Principal> principals) {
        try {
            LabelUtil.singleton().enterTiming();
            if (principals == null || principals.isEmpty()) {
                return topPrincipal();
            }
            if (principals.size() == 1) {
                Object o = principals.iterator().next();
                if (o == null || o instanceof Principal) return (Principal) o;
                return topPrincipal();
            }

            // go through the collection of principals, and flatten them
            Set<Principal> needed = new LinkedHashSet<Principal>();
            for (Principal principal : principals) {
                Object o = principal;
                Principal p = null;
                if (o instanceof Principal) p = (Principal) o;
                if (p == null) return p;
                if (PrincipalUtil.isTopPrincipal(p)) continue;
                if (p instanceof DisjunctivePrincipal) {
                    needed.addAll(((DisjunctivePrincipal) p).disjuncts);
                } else {
                    needed.add(p);
                }
            }
            return new DisjunctivePrincipal(needed);
        } finally {
            LabelUtil.singleton().exitTiming();
        }

    }

    public static Principal conjunction(Collection<Principal> principals) {
        try {
            LabelUtil.singleton().enterTiming();
            if (principals == null || principals.isEmpty()) {
                return bottomPrincipal();
            }
            if (principals.size() == 1) {
                Object o = principals.iterator().next();
                if (o == null || o instanceof Principal) return (Principal) o;
                return bottomPrincipal();
            }

            // go through the collection of principals, and flatten them
            Set<Principal> needed = new LinkedHashSet<Principal>();
            for (Principal principal : principals) {
                Object o = principal;
                Principal p = null;
                if (o instanceof Principal) p = (Principal) o;

                if (p == null) continue; // ignore bottom principals
                if (PrincipalUtil.isTopPrincipal(p)) return p;
                if (p instanceof ConjunctivePrincipal) {
                    needed.addAll(((ConjunctivePrincipal) p).conjuncts);
                } else {
                    needed.add(p);
                }
            }
            return new ConjunctivePrincipal(needed);
        } finally {
            LabelUtil.singleton().exitTiming();
        }

    }

    public static String toString(Principal p) {
        try {
            LabelUtil.singleton().enterTiming();
            return p == null ? "_" : p.name();
        } finally {
            LabelUtil.singleton().exitTiming();
        }

    }

    public static String stringValue(Principal p) {
        try {
            LabelUtil.singleton().enterTiming();
            return toString(p);
        } finally {
            LabelUtil.singleton().exitTiming();
        }

    }

    private static final class TopPrincipal implements Principal {
        private TopPrincipal() {
        }

        @Override
        public String name() {
            return "*";
        }

        @Override
        public boolean delegatesTo(Principal p) {
            return false;
        }

        @Override
        public boolean equals(Principal p) {
            return p == this;
        }

        @Override
        public boolean isAuthorized(Object authPrf, Closure closure, Label lb,
                boolean executeNow) {
            return false;
        }

        @Override
        public ActsForProof findProofUpto(Principal p, Object searchState) {
            return null;
        }

        @Override
        public ActsForProof findProofDownto(Principal q, Object searchState) {
            return null;
        }

    }

    private abstract static class PrincipalPair {
        final Principal p;
        final Principal q;

        PrincipalPair(Principal p, Principal q) {
            this.p = p;
            this.q = q;
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o.getClass().equals(this.getClass())) {
                PrincipalPair that = (PrincipalPair) o;
                return eq(this.p, that.p) && eq(this.q, that.q);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (p == null ? -4234 : p.hashCode())
                    ^ (q == null ? 23 : q.hashCode());
        }

        @Override
        public String toString() {
            return p.name() + "-" + q.name();
        }
    }

    private static class ActsForPair extends PrincipalPair {
        ActsForPair(Principal superior, Principal inferior) {
            super(superior, inferior);
        }
    }

    static class DelegationPair extends PrincipalPair {
        DelegationPair(Principal actor, Principal granter) {
            super(actor, granter);
        }
    }
}
