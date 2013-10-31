package jif.types;

import jif.types.principal.ExternalPrincipal;
import polyglot.types.VarInstance;

/**
 * A <code>PrincipalInstance</code> represents a global principal.
 */
public interface PrincipalInstance extends VarInstance {
    ExternalPrincipal principal();

    PrincipalInstance principal(ExternalPrincipal principal);
}
