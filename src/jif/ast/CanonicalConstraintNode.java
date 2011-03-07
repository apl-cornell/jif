package jif.ast;

import jif.types.*;

/** A canonical(non-ambiguous) constraint node.
 */
public interface CanonicalConstraintNode extends ConstraintNode<Assertion> {
    /** Gets the constraint. */
    public Assertion constraint();
}
