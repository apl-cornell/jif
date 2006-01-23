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
    
    Label join(Label l);
    Label meet(Label l);
    
    ConfPolicy confPolicy();
    IntegPolicy integPolicy();
    
}
