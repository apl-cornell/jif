package jif.types.label;

import java.util.Collection;

import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.LabelEnv.SearchState;

public interface RifJoinConfPolicy extends RifConfPolicy {
    Collection<RifConfPolicy> joinComponents();

    ConfPolicy meet(RifConfPolicy p);

    ConfPolicy join(RifConfPolicy p);

    boolean leq_(Policy p, LabelEnv env, SearchState state);
}
