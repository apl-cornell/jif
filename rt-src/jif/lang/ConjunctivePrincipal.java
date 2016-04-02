package jif.lang;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A conjunction of two or more (non-null) principals
 */
public final class ConjunctivePrincipal implements Principal {
    final Set<Principal> conjuncts;
    private Integer hashCode;

    ConjunctivePrincipal(Set<Principal> conjuncts) {
        this.conjuncts = conjuncts;
    }

    @Override
    public String name() {
        StringBuffer sb = new StringBuffer();
        for (Iterator<Principal> iter = conjuncts.iterator(); iter.hasNext();) {
            Principal p = iter.next();
            sb.append(PrincipalUtil.toString(p));
            if (iter.hasNext()) sb.append("&");
        }
        return sb.toString();
    }

    @Override
    public boolean delegatesTo(Principal p) {
        if (p instanceof ConjunctivePrincipal) {
            ConjunctivePrincipal cp = (ConjunctivePrincipal) p;
            return cp.conjuncts.containsAll(this.conjuncts);
        }
        for (Principal q : conjuncts) {
            if (!PrincipalUtil.delegatesTo(q, p)) return false;
        }
        // every conjuct delegates to p.
        return true;
    }

    @Override
    public int hashCode() {
        if (hashCode == null) {
            hashCode = new Integer(conjuncts.hashCode());
        }
        return hashCode.intValue();
    }

    @Override
    public boolean equals(Principal p) {
        if (p instanceof ConjunctivePrincipal) {
            ConjunctivePrincipal that = (ConjunctivePrincipal) p;
            return this.hashCode() == that.hashCode()
                    && this.conjuncts.equals(that.conjuncts)
                    && that.conjuncts.equals(this.conjuncts);
        }
        return false;
    }

    @Override
    public boolean isAuthorized(Object authPrf, Closure closure, Label lb,
            boolean executeNow) {
        for (Principal p : conjuncts) {
            if (!p.isAuthorized(authPrf, closure, lb, executeNow)) return false;
        }
        // all conjuncts authorize the closure.
        return true;
    }

    @Override
    public ActsForProof findProofUpto(Principal p, Object searchState) {
        Map<Principal, ActsForProof> proofs =
                new HashMap<Principal, ActsForProof>();
        for (Principal q : conjuncts) {
            ActsForProof prf =
                    PrincipalUtil.findActsForProof(p, q, searchState);
            if (prf == null) return null;
            proofs.put(q, prf);
        }

        // proofs contains a proof for every conjunct,
        // which is sufficent for a proof to the conjunctive principal
        return new ToConjunctProof(p, this, proofs);

    }

    @Override
    public ActsForProof findProofDownto(Principal q, Object searchState) {
        for (Principal witness : conjuncts) {
            ActsForProof prf =
                    PrincipalUtil.findActsForProof(witness, q, searchState);
            if (prf != null) {
                // have found a proof from witness to q
                DelegatesProof step = new DelegatesProof(this, witness);
                return new TransitiveProof(step, witness, prf);
            }
        }
        return null;
    }
}
