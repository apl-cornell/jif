package jif.lang;

import java.util.Map;
import java.util.Set;

import jif.lang.PrincipalUtil.DelegationPair;

public final class ToConjunctProof extends ActsForProof {
    private final Map<Principal, ActsForProof> conjunctProofs;

    ToConjunctProof(Principal actor, ConjunctivePrincipal granter,
            Map<Principal, ActsForProof> conjunctProofs) {
        super(actor, granter);
        this.conjunctProofs = conjunctProofs;
    }

    Map<Principal, ActsForProof> getConjunctProofs() {
        return conjunctProofs;
    }

    @Override
    public void gatherDelegationDependencies(Set<DelegationPair> s) {
        ConjunctivePrincipal cp = (ConjunctivePrincipal) getGranter();
        for (Principal conjunct : cp.conjuncts) {
            ActsForProof pr = this.getConjunctProofs().get(conjunct);
            pr.gatherDelegationDependencies(s);
        }
    }
}
