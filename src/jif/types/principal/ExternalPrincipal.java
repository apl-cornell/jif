package jif.types.principal;


/** The external principal existing in the running system. 
 *  It is specified by the name of a principal, such as
 *  "Alice" and "Bob". 
 */
public interface ExternalPrincipal extends Principal {
    String name();
}
