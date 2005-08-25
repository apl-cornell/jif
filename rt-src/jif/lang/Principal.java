package jif.lang;

/**
 * See the doucmentation for the Jif source file, $JIF/sig-src/jif/lang/Principal.jif.
 */
public interface Principal {
    String name();
    
    boolean delegatesTo(final Principal p);
    
    boolean equals(final Principal p);
    
    boolean isAuthorized(final Object authPrf, final Closure closure, final Label lb);
    
    Principal[] findChainUpto(final Principal p);
    
    Principal[] findChainDownto(final Principal q);
}
