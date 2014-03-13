package jif.types.label;

import jif.types.JifTypeSystem;
import jif.types.RifFSM;
import polyglot.ast.Id;
import polyglot.util.Position;

/** The policy label of the form <code>owner -> r1,&period;&period;&period;,rn</code>
 */
public interface RifConfPolicy extends ConfPolicy {

    RifFSM getFSM();

    RifConfPolicy takeTransition(Id action, JifTypeSystem ts, Position pos);
}
