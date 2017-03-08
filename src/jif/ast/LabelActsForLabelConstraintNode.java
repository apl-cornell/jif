package jif.ast;

import jif.types.label.Label;

/**
 * An immutable representation of the Jif <code>ActsFor</code> constraint
 * between two labels.
 * <p>
 * Grammar: <tt>actsFor (actor, granter)</tt>
 * </p>
 * <p>
 * The <tt>ActsFor</tt> constraint only appears in the <tt>where</tt> clause of
 * a procedure header.
 * </p>
 */
public interface LabelActsForLabelConstraintNode
        extends ActsForConstraintNode<Label, Label> {
}
