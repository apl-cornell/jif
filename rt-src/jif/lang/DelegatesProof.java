package jif.lang;

import java.util.HashSet;
import java.util.Set;

import jif.lang.PrincipalUtil.DelegationPair;

public final class DelegatesProof extends ActsForProof {
    public DelegatesProof(Principal actor, Principal granter) {
        super(actor, granter);
    }

    @Override
    public void gatherDelegationDependencies(Set<DelegationPair> s) {
        // don't count delegations from "p" to "p and ..." or delegations from con/dis-junctive principals

        if (getGranter() instanceof DisjunctivePrincipal
                || getGranter() instanceof ConjunctivePrincipal) {
            return;
        }

        if (getActor() instanceof ConjunctivePrincipal
                && ((ConjunctivePrincipal) getActor()).conjuncts
                        .contains(getGranter())) {
            return;
        }
        if (s == null) {
            s = new HashSet<DelegationPair>();
        }
        s.add(new PrincipalUtil.DelegationPair(getActor(), getGranter()));
    }
}
