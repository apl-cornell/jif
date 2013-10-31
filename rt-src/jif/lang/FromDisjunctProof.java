package jif.lang;

import java.util.Map;
import java.util.Set;

import jif.lang.PrincipalUtil.DelegationPair;

public final class FromDisjunctProof extends ActsForProof {
    private final Map<Principal, ActsForProof> disjunctProofs; // map from disjuncts to proofs to Granter

    FromDisjunctProof(DisjunctivePrincipal actor, Principal granter,
            Map<Principal, ActsForProof> disjunctProofs) {
        super(actor, granter);
        this.disjunctProofs = disjunctProofs;
    }

    Map<Principal, ActsForProof> getDisjunctProofs() {
        return disjunctProofs;
    }

    @Override
    public void gatherDelegationDependencies(Set<DelegationPair> s) {
        DisjunctivePrincipal dp = (DisjunctivePrincipal) getActor();
        for (Principal disjunct : dp.disjuncts) {
            ActsForProof pr = this.getDisjunctProofs().get(disjunct);
            pr.gatherDelegationDependencies(s);
        }
    }
}
