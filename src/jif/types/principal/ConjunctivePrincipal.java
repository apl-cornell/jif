package jif.types.principal;


/** 
 * A conjunctive principal represents the conjunction of two principals A&B.
 * The conjunctive principal A&B can act for A and it can act for B.
 */
public interface ConjunctivePrincipal extends Principal {

    Principal conjunctLeft();
    Principal conjunctRight();
}
