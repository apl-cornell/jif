package jif.types.label;

import java.util.Collection;



/** The meet of several labels. 
 */
public interface MeetLabel extends Label
{
    //Label flatten();
    Collection meetComponents();
}
