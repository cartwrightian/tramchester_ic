package com.tramchester.dataimport.loader;

import com.tramchester.dataimport.data.StopData;
import com.tramchester.domain.MutablePlatform;
import com.tramchester.domain.Platform;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.CompositeIdMap;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.Station;

import java.util.Optional;

public class PreloadedStationsAndPlatforms {

    private final CompositeIdMap<Station, MutableStation> stations;
    private final CompositeIdMap<Platform, MutablePlatform> platforms;
    private final TransportEntityFactory factory;

    PreloadedStationsAndPlatforms(final TransportEntityFactory factory) {
        stations = new CompositeIdMap<>();
        platforms = new CompositeIdMap<>();
        this.factory = factory;
    }

    public boolean hasId(final IdFor<Station> stationId) {
        return stations.hasId(stationId);
    }

    public int size() {
        return stations.size();
    }

    public void clear() {
        stations.clear();
    }

    public MutableStation get(final IdFor<Station> stationId) {
        return stations.get(stationId);
    }

    public void createAndAdd(final IdFor<Station> stationId, final StopData stopData) {
        final MutableStation mutableStation = factory.createStation(stationId, stopData);

        final Optional<MutablePlatform> possiblePlatform = factory.maybeCreatePlatform(stopData, mutableStation);

        possiblePlatform.ifPresent(platform -> {
            platforms.add(platform);
            mutableStation.addPlatform(platform);
        });

        stations.add(mutableStation);
    }

    public void updateStation(final IdFor<Station> stationId, final StopData stopData) {
        final MutableStation station = stations.get(stationId);

        final Optional<MutablePlatform> possiblePlatform = factory.maybeCreatePlatform(stopData, station);

        possiblePlatform.ifPresent(platform -> {
            platforms.add(platform);
            station.addPlatform(platform);
        });
    }

    public MutablePlatform getPlatform(final IdFor<Platform> id) {
        return platforms.get(id);
    }

}
