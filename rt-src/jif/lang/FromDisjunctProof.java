package jif.lang;

public final class FromDisjunctProof extends ActsForProof {
    private final ActsForProof leftToGranter;
    private final ActsForProof rightToGranter;
    FromDisjunctProof(DisjunctivePrincipal actor, Principal granter, 
                      ActsForProof leftToGranter, ActsForProof rightToGranter) {
        super(actor, granter);
        this.leftToGranter = leftToGranter;
        this.rightToGranter = rightToGranter;
    }
    ActsForProof getLeftToGranter() {
        return leftToGranter;
    }
    ActsForProof getRightToGranter() {
        return rightToGranter;
    }

}
