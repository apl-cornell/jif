package jif.lang;

import java.util.*;
import java.util.Iterator;
import java.util.Set;

/**
 * A conjunction of two or more (non-null) principals
 */
public final class ConjunctivePrincipal implements Principal {
    final Set conjuncts;
    
    ConjunctivePrincipal(Set conjuncts) {
        this.conjuncts = conjuncts;
    }
    public String name() {
        StringBuffer sb = new StringBuffer();
        for (Iterator iter = conjuncts.iterator(); iter.hasNext(); ) {
            Principal p = (Principal)iter.next(); 
            sb.append(PrincipalUtil.toString(p));
            if (iter.hasNext()) sb.append("&");
        }
        return sb.toString();
    }

    public boolean delegatesTo(Principal p) {
        if (p instanceof ConjunctivePrincipal) {
            ConjunctivePrincipal cp = (ConjunctivePrincipal)p;
            return cp.conjuncts.containsAll(this.conjuncts);
        }
        for (Iterator iter = conjuncts.iterator(); iter.hasNext(); ) {
            Principal q = (Principal)iter.next();
            if (!PrincipalUtil.delegatesTo(q, p)) return false;
        }
        // every conjuct is equal to p.
        return true;
    }

    public boolean equals(Principal p) {
        if (p instanceof ConjunctivePrincipal) {
            ConjunctivePrincipal that = (ConjunctivePrincipal)p;
            return this.conjuncts.equals(that.conjuncts) &&
                that.conjuncts.equals(this.conjuncts);
        }
        return false;
    }

    public boolean isAuthorized(Object authPrf, Closure closure, Label lb) {
        for (Iterator iter = conjuncts.iterator(); iter.hasNext(); ) {
            Principal p = (Principal)iter.next();
            if (!p.isAuthorized(authPrf, closure, lb)) return false;
        }
        // all conjuncts authorize the closure.
        return true;
    }
    

    public ActsForProof findProofUpto(Principal p) {
        Map proofs = new HashMap();
        for (Iterator iter = conjuncts.iterator(); iter.hasNext(); ) {
            Principal q = (Principal)iter.next();
            ActsForProof prf = PrincipalUtil.findActsForProof(p, q);
            if (prf == null) return null;
            proofs.put(q, prf);
        }
        
        // proofs contains a proof for every conjunct,
        // which is sufficent for a proof to the conjunctive principal
        return new ToConjunctProof(p, this, proofs);

    }

    public ActsForProof findProofDownto(Principal q) {
        for (Iterator iter = conjuncts.iterator(); iter.hasNext(); ) {
            Principal witness = (Principal)iter.next();
            ActsForProof prf = PrincipalUtil.findActsForProof(witness, q);
            if (prf != null) {
                // have found a proof from witness to q
                DelegatesProof step = new DelegatesProof(this, witness);
                return new TransitiveProof(step, witness, prf);
            }
        }
        return null;
    }
}