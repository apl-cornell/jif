package jif.types.label;

import java.util.Set;

import jif.types.RifFSM;
import polyglot.ast.Id;

/** The policy label of the form <code>owner -> r1,&period;&period;&period;,rn</code>
 */
public interface RifConfPolicy extends ConfPolicy {

    RifConfPolicy takeTransition(Id action);

    Set<RifFSM> getFSMs();
}
