package jif.types.label;

import java.util.Collection;

/** Represents the join of a number of confidentiality policies. 
 */
public interface JoinConfPolicy extends ConfPolicy, JoinPolicy<ConfPolicy> {
    @Override
    Collection<ConfPolicy> joinComponents();
}
