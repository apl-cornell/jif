package jif.ast;

import java.util.*;

/** A policy label node. A policy label is like <code>owner: r1, r2,...rn</code>. 
 */
public interface PolicyLabelNode extends LabelNode {
    PrincipalNode owner();
    PolicyLabelNode owner(PrincipalNode owner);

    List readers();
    PolicyLabelNode readers(List readers);
}
