package jif.types.label;

import java.util.Collection;

public interface JoinPolicy<P extends Policy> extends Policy {
    Collection<P> joinComponents();
}
