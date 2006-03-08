package jif.lang;

import java.util.*;

/**
 * Utility methods for principals.
 * 
 * See the Jif source code, in lib-src/jif/lang/PrincipalUtil.jif
 */
public class PrincipalUtil {
    private static Map proofCache = new HashMap();    
    private static Principal TOP_PRINCIPAL = new TopPrincipal();

    /**
     * Returns true if and only if the principal p acts for the principal q. A
     * synonym for the <code>actsFor</code> method.
     */
    public static boolean acts_for(Principal p, Principal q) {
        return actsFor(p, q);
    }

    /**
     * Returns true if and only if the principal p acts for the principal q.
     */
    public static boolean actsFor(Principal p, Principal q) {
        // anyone can act for the "null" principal
        if (q == null) return true;
        if (p == TOP_PRINCIPAL) return true;

        // if the two principals are ==-equal, or if they
        // both agree that they are equal to each other, then
        // we return true (since the acts-for relation is
        // reflexive).
        if (p == q) return true;
        if (equals(p,q)) return true;

        // try a simple test first
        if (delegatesTo(q, p)) return true;

        // try the cache
        PrincipalPair pp = new PrincipalPair(p, q);
        ActsForProof prf = (ActsForProof)proofCache.get(pp);
        if (prf != null) {
            if (verifyProof(prf, p, q)) return true;
            // chain is no longer valid
            proofCache.remove(pp);
        }

        // try searching
        prf = findActsForProof(p, q);
        if (prf != null && verifyProof(prf, p, q)) {
            // cache the chain to avoid searching later.
            proofCache.put(pp, prf);
            return true;
        }

        // can't do anything more!
        return false;
    }

    /**
     * Search for an ActsForProof between p and q. An ActsForProof between
     * p and q is a a checkable proof object.
     * @param p
     * @param q
     * @return An ActsForPoorf between p and q, or null if none can be found.
     */
    public static ActsForProof findActsForProof(Principal p, Principal q) {
        // try the dumb things first.
        if (p == q) {
            return new ReflexiveProof(p, q);
        }
        if (q == null) {
            return new DelegatesProof(p, q);            
        }
        if (p != null && p.equals(q) && q.equals(p)) {
            return new ReflexiveProof(p, q);
        }
        
        // if we're going from a dis/conjunctive principal, try finding a downwards
        // proof first
        ActsForProof prf;
        boolean doneDownTo = false;
        if (p instanceof ConjunctivePrincipal || p instanceof DisjunctivePrincipal) {
            prf = p.findProofDownto(q);
            if (prf != null) return prf;            
            doneDownTo = true;
        }

        // try searching upwards from q.
        prf = q.findProofUpto(p);
        if (prf != null) return prf;

        // try searching downards from p.
        if (!doneDownTo && p != null) {
            prf = p.findProofDownto(q);
            if (prf != null) return prf;
        }

        // have failed!
        return null;
    }

    /**
     * Verify that the chain is a valid delegates-chain between p and q. That
     * is, q == chain[n], chain[n] delegates to chain[n-1], ..., chain[0] == p,
     * i.e., p acts for q.
     *  
     */
    public static boolean verifyProof(ActsForProof prf, Principal actor,
            Principal granter) {
        if (prf == null) return false;
        if (prf.getActor() != actor || prf.getGranter() != granter) return false;
        
        if (prf instanceof DelegatesProof) {
            return delegatesTo(granter, actor);
        }
        else if (prf instanceof ReflexiveProof) {
            return actor == granter || (actor != null && granter != null && actor.equals(granter) && granter.equals(actor));
        }
        else if (prf instanceof TransitiveProof) {
            TransitiveProof proof = (TransitiveProof)prf;
            return verifyProof(proof.getActorToP(), proof.getActor(), proof.getP()) &&
                   verifyProof(proof.getPToGranter(), proof.getP(), proof.getGranter());
        }
        else if (prf instanceof FromDisjunctProof) {
            FromDisjunctProof proof = (FromDisjunctProof)prf;
            if (actor instanceof DisjunctivePrincipal) {
                DisjunctivePrincipal dp = (DisjunctivePrincipal)actor;
                // go though each disjunct, and make sure there is a proof
                // from the disjunct to the granter
                for (Iterator iter = dp.disjuncts.iterator(); iter.hasNext(); ) {
                    Principal disjunct = (Principal)iter.next();
                    ActsForProof pr = (ActsForProof)proof.getDisjunctProofs().get(disjunct);
                    if (!verifyProof(pr, disjunct, granter)) return false;
                }
                // we have verified a proof from each disjunct to the granter
                return true;
            }
            
        }
        else if (prf instanceof ToConjunctProof) {
            ToConjunctProof proof = (ToConjunctProof)prf;
            if (granter instanceof ConjunctivePrincipal) {
                ConjunctivePrincipal cp = (ConjunctivePrincipal)granter;
                // go though each conjunct, and make sure there is a proof
                // from actor to the conjunct
                for (Iterator iter = cp.conjuncts.iterator(); iter.hasNext(); ) {
                    Principal conjunct = (Principal)iter.next();
                    ActsForProof pr = (ActsForProof)proof.getConjunctProofs().get(conjunct);
                    if (!verifyProof(pr, actor, conjunct)) return false;
                }
                // we have verified a proof from actor to each conjunct.
                return true;
            }
            
        }
        
        // unknown proof!
        return false;
    }

    public static boolean delegatesTo(Principal granter, Principal superior) {
        if (granter == null) return true;
        if (superior instanceof ConjunctivePrincipal) {
            ConjunctivePrincipal cp = (ConjunctivePrincipal)superior;
            for (Iterator iter = cp.conjuncts.iterator(); iter.hasNext();) {
                Principal conjunct = (Principal)iter.next();
                if (equals(conjunct, granter)) return true;                
            }
        }
        return granter.delegatesTo(superior);
    }
    
    private static class PrincipalPair {
        final Principal p;

        final Principal q;

        PrincipalPair(Principal p, Principal q) {
            this.p = p;
            this.q = q;
        }

        public boolean equals(Object o) {
            if (o instanceof PrincipalPair) {
                PrincipalPair that = (PrincipalPair)o;
                return (this.p == that.p || (this.p != null && that.p != null
                        && this.p.equals(that.p) && that.p.equals(this.p)))
                        && (this.q == that.q || (this.q != null
                                && that.q != null && this.q.equals(that.q) && that.q
                                .equals(this.q)));

            }
            return false;
        }

        public int hashCode() {
            return (p == null ? -4234 : p.hashCode())
                    + (q == null ? 23 : q.hashCode());
        }
    }

    public static boolean equivalentTo(Principal p, Principal q) {
        return actsFor(p, q) && actsFor(q, p);
    }
    public static boolean equals(Principal p, Principal q) {
        return p == q || 
                (p != null && q != null && q.equals(p) &&  p.equals(q));        
    }

    /**
     * Obtain a Capability for the given principal and closure.
     */
    public static Capability authorize(Principal p, Object authPrf, Closure c,
            Label lb) {
        Principal closureP = c.jif$getjif_lang_Closure_P();
        Label closureL = c.jif$getjif_lang_Closure_L();
        if (closureP == p
                || (p != null && closureP != null && p.equals(closureP) && closureP
                        .equals(p))) {
            // The principals agree.
            if (LabelUtil.equivalentTo(closureL, lb)) {
                // the labels agree
                if (p == null || p.isAuthorized(authPrf, c, lb)) {
                    // either p is null (and the "null" principal always
                    // gives authority!) or p grants authority to execute the
                    // closure.
                    return new Capability(closureP, closureL, c);
                }
            }
        }
        return null;
    }
    
    /**
     * returns the null principal, the principal that every other principal can
     * act for.
     */                
    public static Principal nullPrincipal() {
        return null;
    }

    public static Principal bottomPrincipal() {
        return nullPrincipal();
    }
    public static Principal topPrincipal() {
        return TOP_PRINCIPAL;
    }
    static boolean isTopPrincipal(Principal p) {
        return p == TOP_PRINCIPAL;
    }
    public static ConfPolicy readableByPrinPolicy(Principal p) {
        return new ReaderPolicy(topPrincipal(), p);
    }
    
    public static Principal disjunction(Principal left, Principal right) {
        if (left == null || right == null) return null;
        if (actsFor(left, right)) return right;
        if (actsFor(right, left)) return left;
        Collection c = new ArrayList(2);
        c.add(left);
        c.add(right);
        return disjunction(c);
    }
    
    public static Principal conjunction(Principal left, Principal right) {
        if (left == null) return right;
        if (right == null) return left;
        if (actsFor(left, right)) return left;
        if (actsFor(right, left)) return right;
        Collection c = new ArrayList(2);
        c.add(left);
        c.add(right);
        return conjunction(c);
    }
    
    public static Principal disjunction(Collection principals) {        
        if (principals == null || principals.isEmpty()) {
            return topPrincipal();
        }
        if (principals.size() == 1) {
            Object o = principals.iterator().next();
            if (o == null || o instanceof Principal) return (Principal)o;
            return topPrincipal();
        }
        
        // go through the collection of principals, and flatten them
        Set needed = new LinkedHashSet();
        for (Iterator iter = principals.iterator(); iter.hasNext(); ) {
            Object o = iter.next();
            Principal p = null;
            if (o instanceof Principal) p = (Principal)o;
            if (p == null) return p;
            if (PrincipalUtil.isTopPrincipal(p)) continue;
            if (p instanceof DisjunctivePrincipal) {
                needed.addAll(((DisjunctivePrincipal)p).disjuncts);
            }
            else {
                needed.add(p);
            }
        }
        return new DisjunctivePrincipal(needed);
    }
    
    public static Principal conjunction(Collection principals) {        
        if (principals == null || principals.isEmpty()) {
            return bottomPrincipal();
        }
        if (principals.size() == 1) {
            Object o = principals.iterator().next();
            if (o == null || o instanceof Principal) return (Principal)o;
            return bottomPrincipal();
        }
        
        // go through the collection of principals, and flatten them
        Set needed = new LinkedHashSet();
        for (Iterator iter = principals.iterator(); iter.hasNext(); ) {
            Object o = iter.next();
            Principal p = null;
            if (o instanceof Principal) p = (Principal)o;
            
            if (p == null) continue; // ignore bottom principals
            if (PrincipalUtil.isTopPrincipal(p)) return p;
            if (p instanceof ConjunctivePrincipal) {
                needed.addAll(((ConjunctivePrincipal)p).conjuncts);
            }
            else {
                needed.add(p);
            }
        }
        return new ConjunctivePrincipal(needed);
    }

    public static String toString(Principal p) {
        return p== null?"_":p.name();
    }
    public static String stringValue(Principal p) {
        return toString(p);
    }
    
    private static final class TopPrincipal implements Principal {
        private TopPrincipal() { }
        public String name() { return "*"; }
        public boolean delegatesTo(Principal p) { return false; }
        public boolean equals(Principal p) { return p == this; }
        public boolean isAuthorized(Object authPrf, Closure closure, Label lb) { return false; }
        public ActsForProof findProofUpto(Principal p) { return null; }
        public ActsForProof findProofDownto(Principal q) { return null; }
        
    }
}