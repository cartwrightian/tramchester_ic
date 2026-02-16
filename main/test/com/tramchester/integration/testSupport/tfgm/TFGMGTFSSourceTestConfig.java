package com.tramchester.integration.testSupport.tfgm;

import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TemporaryStationsWalkIds;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramDuration;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TFGMGTFSSourceTestConfig implements GTFSSourceConfig {

    //
    // https://data.gov.uk/dataset/c3ca6469-7955-4a57-8bfc-58ef2361b797/gm-public-transport-schedules-new-gtfs-dataset
    //

    private final Set<GTFSTransportationType> sourceModes;
    private final Set<TransportMode> modesWithPlatforms;
    private final ImmutableIdSet<Station> additionalInterchanges;
    private final Set<TransportMode> compositeStationModes;
    private final List<StationClosures> closedStations;
    private final TramDuration maxInitialWait;
    private final List<TemporaryStationsWalkIds> temporaryStationWalks;

    public TFGMGTFSSourceTestConfig(Set<GTFSTransportationType> sourceModes,
                                    Set<TransportMode> modesWithPlatforms, ImmutableIdSet<Station> additionalInterchanges,
                                    Set<TransportMode> compositeStationModes, List<StationClosures> closedStations,
                                    TramDuration maxInitialWait, List<TemporaryStationsWalkIds> temporaryStationWalks) {
        this.sourceModes = sourceModes;
        this.modesWithPlatforms = modesWithPlatforms;
        this.additionalInterchanges = additionalInterchanges;
        this.compositeStationModes = compositeStationModes;
        this.closedStations = closedStations;
        this.maxInitialWait = maxInitialWait;
        this.temporaryStationWalks = temporaryStationWalks;
    }

    public TFGMGTFSSourceTestConfig(GTFSTransportationType mode, TransportMode modeWithPlatform,
                                    ImmutableIdSet<Station> additionalInterchanges, Set<TransportMode> groupStationModes,
                                    List<StationClosures> closedStations, TramDuration maxInitialWait,
                                    List<TemporaryStationsWalkIds> temporaryStationWalks) {
        this(Collections.singleton(mode), Collections.singleton(modeWithPlatform),
                additionalInterchanges, groupStationModes, closedStations, maxInitialWait, temporaryStationWalks);
    }

    @Override
    public String getName() {
        return "tfgm";
    }

    @Override
    public boolean getHasFeedInfo() {
        return false;
    }

    @Override
    public Set<GTFSTransportationType> getTransportGTFSModes() {
        return sourceModes;
    }

    @Override
    public Set<TransportMode> getTransportModesWithPlatforms() {
        return modesWithPlatforms;
    }

    @Override
    public Set<LocalDate> getNoServices() {
        return Collections.emptySet();
    }

    @Override
    public ImmutableIdSet<Station> getAdditionalInterchanges() {
        return additionalInterchanges;
    }

    @Override
    public Set<TransportMode> groupedStationModes() {
        return compositeStationModes;
    }

    @Override
    public List<StationClosures> getStationClosures() {
        return closedStations;
    }

    @Override
    public boolean getAddWalksForClosed() {
        return true;
    }

    @Override
    public List<TemporaryStationsWalkIds> getTemporaryStationWalks() {
        return temporaryStationWalks;
    }

    @Override
    public boolean getOnlyMarkedInterchanges() {
        return false;
    }

    @Override
    public TramDuration getMaxInitialWait() {
        return maxInitialWait;
    }
}
