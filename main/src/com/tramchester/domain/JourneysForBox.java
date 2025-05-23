package com.tramchester.domain;

import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.GridPosition;

import java.util.Comparator;
import java.util.Set;

public class JourneysForBox {
    private final BoundingBoxWithStations box;
    private final Set<Journey> journeys;

    public JourneysForBox(BoundingBoxWithStations box, Set<Journey> journeys) {
        this.box = box;
        this.journeys = journeys;
    }

    public Set<Journey> getJourneys() {
        return journeys;
    }

    public BoundingBoxWithStations getBox() {
        return box;
    }

    @Override
    public String toString() {
        return "JourneysForBox{" +
                "box=" + box +
                ", journeys=" + journeys +
                '}';
    }

    public boolean contains(final GridPosition destination) {
        return box.contained(destination);
    }

    public boolean isEmpty() {
        return journeys.isEmpty();
    }

    public Journey getLowestCost() {
        return journeys.stream().min(Comparator.comparing(Journey::getArrivalTime)).
                orElseThrow(() -> new RuntimeException("Journeys empty"));
    }
}
