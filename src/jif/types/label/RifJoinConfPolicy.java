package jif.types.label;

import java.util.Collection;

import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.LabelEnv.SearchState;

public interface RifJoinConfPolicy extends ConfPolicy {
    Collection<RifReaderPolicy> joinComponents();

    ConfPolicy meet(RifReaderPolicy p);

    ConfPolicy join(RifReaderPolicy p);

    boolean leq_(Policy p, LabelEnv env, SearchState state);
}
