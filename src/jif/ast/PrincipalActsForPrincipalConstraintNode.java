package jif.ast;

import jif.types.principal.Principal;

/**
 * An immutable representation of the Jif <code>ActsFor constraint</code>
 * between two principals.
 * <p>
 * Grammar: <tt>actsFor (actor, granter)</tt>
 * </p>
 * <p>
 * The <tt>ActsFor constraint</tt> only appears in the <tt>where</tt> clause of
 * a procedure header.
 * </p>
 */
public interface PrincipalActsForPrincipalConstraintNode
        extends ActsForConstraintNode<Principal, Principal> {
}
