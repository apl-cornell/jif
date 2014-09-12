package jif.types.label;

import java.util.Collection;

import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.LabelEnv.SearchState;

public interface RifJoinIntegPolicy extends RifIntegPolicy {
    Collection<IntegPolicy> joinComponents();

    boolean leq_(Policy p, LabelEnv env, SearchState state);

    IntegPolicy flatten();

}
