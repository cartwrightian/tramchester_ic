package com.tramchester.testSupport.reference;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutablePlatform;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NPTGLocality;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.StationRepository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface FakeStation extends HasId<Station> {

    @Override
    default IdFor<Station> getId() {
        return Station.createId(getRawId());
    }

    default boolean matches(IdFor<Station> id) {
        return getId().equals(id);
    }

    String getName();

    LatLong getLatLong();

    String getRawId();

    default Station from(StationRepository stationRepository) {
        return stationRepository.getStationById(getId());
    }

    default LocationId<Station> getLocationId() {
        return LocationId.wrap(getId());
    }

    default IdForDTO getIdForDTO() {
        return new IdForDTO(getRawId());
    }

    TransportMode getMode();

    DataSourceID getDatasourceId();

    Station fake();

    default Station fakeWithPlatform(final int platformNumber, TramDate date) {
        return faker().platform(platformNumber, date).build();
    }

    default Station fake(final TestRoute knownTramRoute) {
        return faker().dropOff(knownTramRoute).build();
    }

    default FakeStationBuilder faker() {
        return new FakeStationBuilder(this);
    }

    class FakeStationBuilder {
        private final Map<Integer, TestRoute> fakeDropOffPlatforms;
        private final Set<TestRoute> fakeRoutes;
        private final FakeStation fakeStation;

        public FakeStationBuilder(final FakeStation fakeStation) {
            this.fakeStation = fakeStation;
            fakeDropOffPlatforms = new HashMap<>();
            fakeRoutes = new HashSet<>();
        }

        public FakeStationBuilder dropOff(final TestRoute knownTramRoute) {
            fakeRoutes.add(knownTramRoute);
            return this;
        }

        public FakeStationBuilder platform(final int platformNumber, TramDate date) {
            fakeDropOffPlatforms.put(platformNumber, KnownTramRoute.getPink(date));
            return this;
        }

        public FakeStationBuilder dropOffPlatform(final int platformNumber, final TestRoute route) {
            if (fakeDropOffPlatforms.containsKey(platformNumber)) {
                throw new RuntimeException("Platform " + platformNumber + " already seen for route " + fakeDropOffPlatforms.get(platformNumber));
            }
            fakeDropOffPlatforms.put(platformNumber, route);
            return this;
        }

        public Station build() {
            final MutableStation station = createMutable();

            final Set<Route> routes = fakeRoutes.stream().map(TestRoute::fake).collect(Collectors.toSet());
            final Set<Platform> platforms = fakeDropOffPlatforms.entrySet().stream().
                    map(entry -> createPlatform(station, entry.getKey()).addRouteDropOff(entry.getValue().fake())).
                    collect(Collectors.toSet());

            platforms.forEach(station::addPlatform);
            routes.forEach(station::addRouteDropOff);

            return station;
        }

        // into builder
        private MutableStation createMutable() {

            final LatLong latLong = fakeStation.getLatLong();
            final GridPosition grid = CoordinateTransforms.getGridPosition(latLong);
            final MutableStation mutableStation = new FakeMutableStation(fakeStation.getId(), NPTGLocality.InvalidId(),
                    fakeStation.getName(), latLong, grid, fakeStation.getDatasourceId(), true);
            mutableStation.addMode(fakeStation.getMode());
            return mutableStation;
        }

        private MutablePlatform createPlatform(final Station station, Integer platformNumber) {
            final PlatformId platformId = PlatformId.createId(station, platformNumber.toString());
            return MutablePlatform.buildForTFGMTram(platformId, station,
                    station.getLatLong(), station.getDataSourceID(), station.getLocalityId());
        }

    }

    class FakeMutableStation extends MutableStation {

        public FakeMutableStation(IdFor<Station> id, IdFor<NPTGLocality> localityId, String stationName, LatLong latLong,
                                  GridPosition gridPosition, DataSourceID dataSourceID, boolean isCentral) {
            super(id, localityId, stationName, latLong, gridPosition, dataSourceID, isCentral);
        }
    }
}
