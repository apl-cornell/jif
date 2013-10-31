package jif.types;

import java.util.List;

import jif.types.principal.Principal;

/** The authority constraint. 
 */
public interface AuthConstraint extends Assertion {
    List<Principal> principals();

    AuthConstraint principals(List<Principal> principals);
}
