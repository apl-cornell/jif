package jif.lang;

/**
 * A conjunction of two (non-null) principals
 */
public final class ConjunctivePrincipal implements Principal {
    final Principal conjunctL;
    final Principal conjunctR;
    
    ConjunctivePrincipal(Principal conjunctL, Principal conjunctR) {
        this.conjunctL = conjunctL;
        this.conjunctR = conjunctR;
    }
    public String name() {
        return conjunctL.name() + "&" + conjunctR.name();
    }

    public boolean delegatesTo(Principal p) {
        return conjunctL.delegatesTo(p) && conjunctR.delegatesTo(p);
    }

    public boolean equals(Principal p) {
        if (p instanceof ConjunctivePrincipal) {
            ConjunctivePrincipal that = (ConjunctivePrincipal)p;
            return this.conjunctL.equals(that.conjunctL) &&
                this.conjunctR.equals(that.conjunctR);
        }
        return false;
    }

    public boolean isAuthorized(Object authPrf, Closure closure, Label lb) {
        return conjunctL.isAuthorized(authPrf, closure, lb) && conjunctR.isAuthorized(authPrf, closure, lb);
    }

    public ActsForProof findProofUpto(Principal p) {
        ActsForProof prfLeft = conjunctL.findProofUpto(p);
        if (prfLeft == null) return null;
        
        ActsForProof prfRight = conjunctR.findProofUpto(p);
        if (prfRight == null) return null;
        
        // there is a proof from p to left, and from p to right, which
        // is enough to make a proof to this conjunct.
        return new ToConjunctProof(p, this, prfLeft, prfRight);
    }

    public ActsForProof findProofDownto(Principal q) {
        Principal witness = conjunctL;
        ActsForProof prf = conjunctL.findProofDownto(q);
        
        if (prf == null) {
            witness = conjunctR;
            prf = conjunctR.findProofDownto(q);
        }
        
        if (prf == null) return null;
        
        // have found a proof from one of left or right down to q.
        // prf is now a proof from witness to q
        DelegatesProof step = new DelegatesProof(this, witness);
        return new TransitiveProof(step, witness, prf);
    }
}
