package jif.ast;

import jif.types.label.Label;
import jif.types.principal.Principal;

/**
 * An immutable representation of the Jif <code>ActsFor</code> constraint
 * between a label and a principal.
 * <p>
 * Grammar: <tt>actsFor (actor, granter)</tt>
 * </p>
 * <p>
 * The <tt>ActsFor</tt> constraint only appears in the <tt>where</tt> clause of
 * a procedure header.
 * </p>
 */
public interface LabelActsForPrincipalConstraintNode
        extends ActsForConstraintNode<Label, Principal> {
}
