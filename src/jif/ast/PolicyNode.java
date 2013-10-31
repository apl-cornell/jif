package jif.ast;

import jif.types.label.Policy;

/** A policy label node. A policy label is like <code>owner: r1, r2,...rn</code>. 
 */
public interface PolicyNode extends LabelComponentNode {
    // if the node is disambiguated, then this should return a non-null value.
    Policy policy();

    PrincipalNode owner();

    PolicyNode owner(PrincipalNode owner);
}
