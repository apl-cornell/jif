package jif.lang;

import java.util.Set;

import jif.lang.PrincipalUtil.DelegationPair;

public final class TransitiveProof extends ActsForProof {
    private final ActsForProof actorToP;
    private final ActsForProof pToGranter;
    private final Principal p;

    public TransitiveProof(ActsForProof actorToP, Principal p,
            ActsForProof pToGranter) {
        super(actorToP != null ? actorToP.getActor() : null,
                pToGranter != null ? pToGranter.getGranter() : null);
        this.actorToP = actorToP;
        this.pToGranter = pToGranter;
        this.p = p;
    }

    ActsForProof getActorToP() {
        return actorToP;
    }

    ActsForProof getPToGranter() {
        return pToGranter;
    }

    Principal getP() {
        return p;
    }

    @Override
    public void gatherDelegationDependencies(Set<DelegationPair> s) {
        actorToP.gatherDelegationDependencies(s);
        pToGranter.gatherDelegationDependencies(s);
    }

}
