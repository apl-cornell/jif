package jif.types.label;

import java.util.Collection;

public interface MeetPolicy<P extends Policy> {
    Collection<P> meetComponents();
}
