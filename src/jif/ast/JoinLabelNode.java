package jif.ast;

import java.util.*;

/** This class represents a join of several label nodes. It's also a
 *  label node by itself. 
 */
public interface JoinLabelNode extends LabelNode {
    /** Gets the list of join components. */
    List components();
    
    /** Returns a copy of this object with <code>components</code> updated.
     */    
    JoinLabelNode components(List components);
}
