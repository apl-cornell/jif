package jif.lang;

public final class ToConjunctProof extends ActsForProof {
    private final ActsForProof actorToLeft;
    private final ActsForProof actorToRight;
    ToConjunctProof(Principal actor, ConjunctivePrincipal granter,
                    ActsForProof actorToLeft, ActsForProof actorToRight) {
        super(actor, granter);
        this.actorToLeft = actorToLeft;
        this.actorToRight = actorToRight;
    }
    ActsForProof getActorToLeft() {
        return actorToLeft;
    }
    ActsForProof getActorToRight() {
        return actorToRight;
    }

}
