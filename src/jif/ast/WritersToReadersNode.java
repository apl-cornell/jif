package jif.ast;

/** This class represents a join of several label nodes. It's also a
 *  label node by itself. 
 */
public interface WritersToReadersNode extends PolicyNode {
    /**
     * Gets the label component that the writers to readers transformation is
     * being performed on.
     */
    LabelComponentNode component();

    /**
     * Returns a copy of this object with <code>sublabel</code> updated.
     */
    WritersToReadersNode component(LabelComponentNode component);
}
