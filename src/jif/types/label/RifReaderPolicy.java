package jif.types.label;

import java.util.List;

import jif.types.RifComponent;
import jif.types.RifFSM;

/** The policy label of the form <code>owner -> r1,&period;&period;&period;,rn</code>
 */
public interface RifReaderPolicy extends ConfPolicy {
    public List<RifComponent> components();

    RifFSM getFSM();
}
