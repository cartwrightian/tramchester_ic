package com.tramchester.domain;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.reference.TransportMode;

public interface HasTransportModes {
    ImmutableEnumSet<TransportMode> getTransportModes();
    boolean anyOverlapWith(ImmutableEnumSet<TransportMode> modes);
}
