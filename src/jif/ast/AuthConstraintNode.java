package jif.ast;

import java.util.List;

import jif.types.AuthConstraint;

/** An authority constraint node. It represents an authority
 *  constraint of a method or a class. 
 *  <p>Grammar: <tt>authority(principal_list)</tt>
 */
public interface AuthConstraintNode extends ConstraintNode<AuthConstraint> {
    /** Gets the list of principal who grants their authorities. */
    List<PrincipalNode> principals();

    /** Returns a copy of this node with the principal list updated. */
    AuthConstraintNode principals(List<PrincipalNode> principals);
}
