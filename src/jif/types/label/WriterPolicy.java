package jif.types.label;

import java.util.Collection;

import jif.types.principal.Principal;

/** The policy label of the form <code>owner!: w1,&period;&period;&period;,wn</code>
 */
public interface WriterPolicy extends Policy {
    Principal owner();
    Principal writer();
}

