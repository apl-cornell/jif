package jif.lang;


/**
 * A Principal is a runtime representation of a principal.
 * See sig-src/jif/lang/Principal.jif.
 */
public interface Principal {
    String name();
    
    boolean delegatesTo(Principal p);
    
    boolean isAuthorized(Object authorizationProof, Closure closure);
}
