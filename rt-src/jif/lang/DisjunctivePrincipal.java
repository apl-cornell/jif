package jif.lang;

import java.util.*;

/**
 * A disjunction of two (non-null) principals
 */
public final class DisjunctivePrincipal implements Principal {
    final Set disjuncts;
    
    DisjunctivePrincipal(Set disjuncts) {
        this.disjuncts = disjuncts;
    }
    public String name() {
        StringBuffer sb = new StringBuffer();
        for (Iterator iter = disjuncts.iterator(); iter.hasNext(); ) {
            Principal p = (Principal)iter.next(); 
            sb.append(PrincipalUtil.toString(p));
            if (iter.hasNext()) sb.append(",");
        }
        return sb.toString();
    }

    public boolean delegatesTo(Principal p) {
        if (p instanceof DisjunctivePrincipal) {
            DisjunctivePrincipal dp = (DisjunctivePrincipal)p;
            return this.disjuncts.containsAll(dp.disjuncts);
        }
        for (Iterator iter = disjuncts.iterator(); iter.hasNext(); ) {
            Principal q = (Principal)iter.next();
            if (PrincipalUtil.equals(q, p)) return true;
        }
        return false;
    }

    public boolean equals(Principal p) {
        if (p instanceof DisjunctivePrincipal) {
            DisjunctivePrincipal that = (DisjunctivePrincipal)p;
            return this.disjuncts.equals(that.disjuncts) &&
                that.disjuncts.equals(this.disjuncts);
        }
        return false;
    }

    public boolean isAuthorized(Object authPrf, Closure closure, Label lb) {
        for (Iterator iter = disjuncts.iterator(); iter.hasNext(); ) {
            Principal p = (Principal)iter.next();
            if (p.isAuthorized(authPrf, closure, lb)) return true;
        }
        return false;
    }

    public ActsForProof findProofUpto(Principal p) {
        if (delegatesTo(p)) {
            return new DelegatesProof(p, this);
        }
        for (Iterator iter = disjuncts.iterator(); iter.hasNext(); ) {
            Principal witness = (Principal)iter.next();
            ActsForProof prf = PrincipalUtil.findActsForProof(p, witness);
            if (prf != null) {
                // have found a proof from p to witness.
                DelegatesProof step = new DelegatesProof(witness, this);
                return new TransitiveProof(prf, witness, step);                
            }
        }
        return null;
    }

    public ActsForProof findProofDownto(Principal q) {
        Map proofs = new HashMap();
        for (Iterator iter = disjuncts.iterator(); iter.hasNext(); ) {
            Principal p = (Principal)iter.next();
            ActsForProof prf = PrincipalUtil.findActsForProof(p, q);
            if (prf == null) return null;
            proofs.put(p, prf);
        }
        
        // proofs contains a proof for every disjunct,
        // which is sufficent for a proof for the disjunctive principal
        return new FromDisjunctProof(this, q, proofs);
    }
}