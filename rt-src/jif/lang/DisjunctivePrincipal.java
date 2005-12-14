package jif.lang;

/**
 * A disjunction of two (non-null) principals
 */
public final class DisjunctivePrincipal implements Principal {
    final Principal disjunctL;
    final Principal disjunctR;
    
    DisjunctivePrincipal(Principal disjunctL, Principal disjunctR) {
        this.disjunctL = disjunctL;
        this.disjunctR = disjunctR;
    }
    public String name() {
        return disjunctL.name() + "," + disjunctR.name();
    }

    public boolean delegatesTo(Principal p) {
        return disjunctL.equals(p) || disjunctR.equals(p);
    }

    public boolean equals(Principal p) {
        if (p instanceof DisjunctivePrincipal) {
            DisjunctivePrincipal that = (DisjunctivePrincipal)p;
            return this.disjunctL.equals(that.disjunctL) &&
                this.disjunctR.equals(that.disjunctR);
        }
        return false;
    }

    public boolean isAuthorized(Object authPrf, Closure closure, Label lb) {
        return disjunctL.isAuthorized(authPrf, closure, lb) || disjunctR.isAuthorized(authPrf, closure, lb);
    }

    public ActsForProof findProofUpto(Principal p) {
        Principal witness = disjunctL;
        ActsForProof prf = disjunctL.findProofUpto(p);
        if (prf == null) {
            witness = disjunctR;
            prf = disjunctR.findProofUpto(p);
        }
        if (prf == null) return null;
        
        // have found a proof from q to one of left or right.
        // prf is now a proof from p to witness
        DelegatesProof step = new DelegatesProof(witness, this);
        return new TransitiveProof(prf, witness, step);
    }

    public ActsForProof findProofDownto(Principal q) {
        ActsForProof prfLeft = disjunctL.findProofDownto(q);
        if (prfLeft == null) return null;
        
        ActsForProof prfRight = disjunctR.findProofDownto(q);
        if (prfRight == null) return null;
        
        // there is a proof from left to q, and from right to q, which
        // is enough to make a proof from this to q.
        return new FromDisjunctProof(this, q, prfLeft, prfRight);
    }
}
