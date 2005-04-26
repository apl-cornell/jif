package jif.lang;

import java.util.Set;

/**
 * A Principal is a runtime representation of a principal.
 */
public interface Principal {
    boolean actsFor(Principal principal);
    boolean equivalentTo(Principal principal);
    String name();
    String fullName();

    /**
     * Set of the Principals that are the immediate superiors of this
     * principal, that is, any member of this set may act for this principal.
     */
    Set superiors();
}
