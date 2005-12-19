package jif.lang;

import java.util.Map;

public final class FromDisjunctProof extends ActsForProof {
    private final Map disjunctProofs; // map from disjuncts to proofs to Granter 
    FromDisjunctProof(DisjunctivePrincipal actor, Principal granter, 
                      Map disjunctProofs) {
        super(actor, granter);
        this.disjunctProofs = disjunctProofs;
    }
    Map getDisjunctProofs() {
        return disjunctProofs;
    }
}
