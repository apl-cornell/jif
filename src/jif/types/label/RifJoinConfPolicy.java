package jif.types.label;

import java.util.Collection;

import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.LabelEnv.SearchState;

public interface RifJoinConfPolicy extends RifConfPolicy {
    Collection<ConfPolicy> joinComponents();

    boolean leq_(Policy p, LabelEnv env, SearchState state);

    ConfPolicy flatten();

}