package jif.types.label;

import jif.types.Param;

/** 
 * This class represents the Jif security label.
 */
public interface Label extends Param
{
    /**
     * Is this label equivalent to bottom?
     * <p>
     * For example, a JoinLabel with no components would return true for this 
     * method.
     */
    boolean isBottom();
    
    /**
     * Is this label equivalent to top?
     * <p>
     *  For example, a JoinLabel with two components, one of which is Top, would 
     *  return true for this method.
     */
    boolean isTop(); 
    
    /**
     * Is this label invariant?
     */
    boolean isInvariant();

    /**
     * Is this label covariant?
     */
    boolean isCovariant();

    /** 
     * Returns the join of this label and L. 
     */
    Label join(Label L);
    
    /**
     * Is this label comparable to other labels?
     * <p>
     * For example, an UnknownLabel is not comparable to others, neither is a VarLabel.
     * Most other labels are. 
     */
    boolean isComparable();
    
    String description();
    
    void  setDescription(String d);
}
