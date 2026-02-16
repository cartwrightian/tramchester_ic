package com.tramchester.domain;

import com.tramchester.domain.time.TramDuration;
import com.tramchester.geo.BoundingBox;


public class BoundingBoxWithCost extends BoundingBox {

    private final TramDuration duration;
    private final Journey journey;

    public BoundingBoxWithCost(BoundingBox box, TramDuration duration, Journey journey) {
        super(box);
        this.duration = duration;
        this.journey = journey;
    }

    public TramDuration getDuration() {
        return duration;
    }

    public Journey getJourney() {
        return journey;
    }
}
