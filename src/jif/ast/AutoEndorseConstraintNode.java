package jif.ast;

import jif.types.AutoEndorseConstraint;

/** An auto endorse constraint node. It automatically endorses the
 * initial pc of the method body to the specified label.
 */
public interface AutoEndorseConstraintNode
        extends ConstraintNode<AutoEndorseConstraint> {

    LabelNode endorseTo();

    AutoEndorseConstraintNode endorseTo(LabelNode endorseTo);
}
