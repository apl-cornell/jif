package jif.types.label;

import jif.types.RifFSM;

/** The policy label of the form <code>owner -> r1,&period;&period;&period;,rn</code>
 */
public interface RifReaderPolicy extends ConfPolicy {

    RifFSM getFSM();
}
