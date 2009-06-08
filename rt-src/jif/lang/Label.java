package jif.lang;

import java.util.Set;


/**
 * A Label is the runtime representation of a Jif label. 
 *  
 */
public interface Label
{
    /**
     * Returns true iff this <= l. If the method returns true, then
     * s has all of the delegations (i.e., DelegationPairs) added to it
     * that the result depends upon. If the method returns false,
     * then s has no elements added to it.
     */
    boolean relabelsTo(Label l, Set s);
    
    Label join(Label l);
    Label meet(Label l);
    
    ConfPolicy confPolicy();
    IntegPolicy integPolicy();
    
}
