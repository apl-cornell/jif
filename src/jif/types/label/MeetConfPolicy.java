package jif.types.label;

import java.util.Collection;

public interface MeetConfPolicy extends ConfPolicy, MeetPolicy<ConfPolicy> {
    @Override
    Collection<ConfPolicy> meetComponents();
}
