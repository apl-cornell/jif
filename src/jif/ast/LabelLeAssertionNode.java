package jif.ast;

import jif.types.LabelLeAssertion;

public interface LabelLeAssertionNode extends ConstraintNode<LabelLeAssertion> {
    /** Gets the lhs Label. */
    LabelNode lhs();

    /** Returns a copy of this node with the lhs updated. */
    LabelLeAssertionNode lhs(LabelNode lhs);

    /** Gets the rhs Label. */
    LabelNode rhs();

    /** Returns a copy of this node with the rhs updated. */
    LabelLeAssertionNode rhs(LabelNode rhs);
}
