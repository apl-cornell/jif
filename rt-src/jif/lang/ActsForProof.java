package jif.lang;

public abstract class ActsForProof {
    private final Principal actor;
    private final Principal granter;
    
    ActsForProof(Principal actor, Principal granter) {
        this.actor = actor;
        this.granter = granter;
    }

    public Principal getActor() {
        return actor;
    }
    public Principal getGranter() {
        return granter;
    }

}
