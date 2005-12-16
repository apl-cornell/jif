package jif.lang;

import java.util.*;

/**
 * A Policy is a component of a label, and is either an integrity policy or
 * a confidentiatlity policy. 
 *  
 */
public interface Policy
{
    boolean relabelsTo(Policy l);    
    String componentString();
}
