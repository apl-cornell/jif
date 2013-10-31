package jif.types;

import java.util.List;

import jif.types.principal.Principal;

/** The caller constraint. 
 */
public interface CallerConstraint extends Assertion {
    List<Principal> principals();

    CallerConstraint principals(List<Principal> principals);
}
