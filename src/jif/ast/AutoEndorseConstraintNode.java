package jif.ast;

import java.util.List;

/** An auto endorse constraint node. It automatically endorses the
 * initial pc of the method body to be trusted by each of the principals
 * in the principal list.
 */
public interface AutoEndorseConstraintNode extends ConstraintNode
{
    /**Gets the list of principals who auto endorse the method body. */
    List principals();
    
    /**Returns a copy of this node with the principal list updated. */
    AutoEndorseConstraintNode principals(List principals);
}
