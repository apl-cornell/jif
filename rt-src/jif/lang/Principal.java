package jif.lang;


/**
 * A Principal is a runtime representation of a principal.
 * See sig-src/jif/lang/Principal.jif.
 */
public interface Principal {
    String name();
    
    boolean delegatesTo(Principal p);
    
    boolean isAuthorized(Object authorizationProof, Closure closure, Label lb);

    boolean equals(Principal p);

	/**
     * Search for a chain of principals <code>a</code> of length L such that
     *   <pre>
     *     a[L-1] == this
     *     a[L-1] delegates to a[L-2]
     *     ...
     *     a[1] delegates to a[0]
     *     a[0] == p
     *   </pre>.
     * 
     * A class implementing this may assume that this.delegatesTo(p) returns false.
     *  
     * Any class implementing this interface should attempt to return non-null
     * values from at least one of the two methods
     * <code>findChainUpto(Principal)</code> and 
     * <code>findChainDownto(Principal)</code>. 
     * 
     * @return a chain as described above, or <code>null</code> 
     *     if no such chain can be found.   
     */
    Principal[] findChainUpto(Principal p);

    /**
     * Search for a chain of principals <code>a</code> of length L such that
     *   <pre>
     *     a[L-1] == q
     *     a[L-1] delegates to a[L-2]
     *     ...
     *     a[1] delegates to a[0]
     *     a[0] == this
     *   </pre>.
     *  
     * A class implementing this may assume that q.delegatesTo(this) returns false.
     * 
     * Any class implementing this interface should attempt to return non-null
     * values from at least one of the two methods
     * <code>findChainUpto(Principal)</code> and 
     * <code>findChainDownto(Principal)</code>. 
     * 
     * @return a chain as described above, or <code>null</code> 
     *     if no such chain can be found.   
     */
    Principal[] findChainDownto(Principal q);
}
