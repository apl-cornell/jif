package jif.lang;

import java.util.*;

/**
 * A Label is the runtime representation of a Jif label. 
 *  
 */
public interface Label
{
    /**
     * Returns true iff this <= l
     */
    boolean relabelsTo(Label l);
    
    /**
     * Returns true iff this <= l
     */
    Label join(Label l);

    Set joinComponents();
    
    /**
     * String to print if this label is a component of a larger label
     */
    String componentString();
}
