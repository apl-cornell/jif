package jif.lang;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A disjunction of two (non-null) principals
 */
public final class DisjunctivePrincipal implements Principal {
    final Set<Principal> disjuncts;
    private Integer hashCode = null;

    DisjunctivePrincipal(Set<Principal> disjuncts) {
        this.disjuncts = disjuncts;
    }

    @Override
    public String name() {
        StringBuffer sb = new StringBuffer();
        for (Iterator<Principal> iter = disjuncts.iterator(); iter.hasNext();) {
            Principal p = iter.next();
            sb.append(PrincipalUtil.toString(p));
            if (iter.hasNext()) sb.append(",");
        }
        return sb.toString();
    }

    @Override
    public boolean delegatesTo(Principal p) {
        if (p instanceof DisjunctivePrincipal) {
            DisjunctivePrincipal dp = (DisjunctivePrincipal) p;
            return this.disjuncts.containsAll(dp.disjuncts);
        }
        for (Principal q : disjuncts) {
            if (PrincipalUtil.equals(q, p)) return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (hashCode == null) {
            hashCode = new Integer(disjuncts.hashCode());
        }
        return hashCode.intValue();
    }

    @Override
    public boolean equals(Principal p) {
        if (p instanceof DisjunctivePrincipal) {
            DisjunctivePrincipal that = (DisjunctivePrincipal) p;
            return this.hashCode() == that.hashCode()
                    && this.disjuncts.equals(that.disjuncts)
                    && that.disjuncts.equals(this.disjuncts);
        }
        return false;
    }

    @Override
    public boolean isAuthorized(Object authPrf, Closure closure, Label lb,
            boolean executeNow) {
        for (Principal p : disjuncts) {
            if (p.isAuthorized(authPrf, closure, lb, executeNow)) return true;
        }
        return false;
    }

    @Override
    public ActsForProof findProofUpto(Principal p, Object searchState) {
        if (delegatesTo(p)) {
            return new DelegatesProof(p, this);
        }
        for (Principal witness : disjuncts) {
            ActsForProof prf =
                    PrincipalUtil.findActsForProof(p, witness, searchState);
            if (prf != null) {
                // have found a proof from p to witness.
                DelegatesProof step = new DelegatesProof(witness, this);
                return new TransitiveProof(prf, witness, step);
            }
        }
        return null;
    }

    @Override
    public ActsForProof findProofDownto(Principal q, Object searchState) {
        Map<Principal, ActsForProof> proofs =
                new HashMap<Principal, ActsForProof>();
        for (Principal p : disjuncts) {
            ActsForProof prf =
                    PrincipalUtil.findActsForProof(p, q, searchState);
            if (prf == null) return null;
            proofs.put(p, prf);
        }

        // proofs contains a proof for every disjunct,
        // which is sufficent for a proof for the disjunctive principal
        return new FromDisjunctProof(this, q, proofs);
    }
}
