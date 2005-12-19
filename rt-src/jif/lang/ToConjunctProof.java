package jif.lang;

import java.util.Map;

public final class ToConjunctProof extends ActsForProof {
    private final Map conjunctProofs;
    ToConjunctProof(Principal actor, ConjunctivePrincipal granter,
                    Map conjunctProofs) {
        super(actor, granter);
        this.conjunctProofs = conjunctProofs;
    }
    Map getConjunctProofs() {
        return conjunctProofs;
    }
}
