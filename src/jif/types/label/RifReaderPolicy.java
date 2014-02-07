package jif.types.label;

import java.util.List;

import jif.ast.RifComponentNode;

/** The policy label of the form <code>owner -> r1,&period;&period;&period;,rn</code>
 */
public interface RifReaderPolicy extends ConfPolicy {
    public List<RifComponentNode> components();
}
