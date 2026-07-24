package com.tramchester.livedata.domain.liveUpdates;

import com.tramchester.domain.Agency;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ImmutableIdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class UpcomingDeparture {

    private final LocalDate date;
    private final String carriages; // double/single
    private final String status; // due, arrived, etc
    private final Station displayLocation;
    private final Station destination;
    private final TramTime when;
    private final Agency agency;
    private final TransportMode mode;
    private Platform platform;
    private final ImmutableIdSet<Station> callingPoints;

    public static final Set<String> KNOWN_TRAM_STATUS = new HashSet<>(Arrays.asList("Due", "Arrived", "Departing"));

    // TODO Should have Calling Points as List??

    public UpcomingDeparture(LocalDate date, Station displayLocation, Station destination, String status, TramTime when,
                             String carriages, Agency agency, TransportMode mode) {
        this(date, displayLocation, destination, status, when, carriages, agency, mode, IdSet.emptySet());
    }

    public UpcomingDeparture(LocalDate date, Station displayLocation, Station destination, String status, TramTime when,
                             String carriages, Agency agency, TransportMode mode, ImmutableIdSet<Station> callingPoints) {
        this.date = date;
        this.displayLocation = displayLocation;
        this.destination = destination;
        this.status = status;
        this.carriages = carriages;
        this.agency = agency;
        this.mode = mode;
        this.when  = when;
        this.callingPoints = callingPoints;
        platform = null;
    }

    public String getDestinationName() {
        return destination.getName();
    }

    public IdFor<Station> getDestinationId() {
        return destination.getId();
    }

    public String getStatus() {
        return status;
    }

    public String getCarriages() {
        return carriages;
    }

    public TramTime getWhen() {
        return when;
    }

    public Station getDisplayLocation() {
        return displayLocation;
    }

    public Agency getAgency() {
        return agency;
    }

    public TransportMode getMode() {
        return mode;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public boolean hasPlatform() {
        return platform!=null;
    }

    public Platform getPlatform() {
        return platform;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UpcomingDeparture that = (UpcomingDeparture) o;
        return Objects.equals(date, that.date) && Objects.equals(carriages, that.carriages)
                && Objects.equals(status, that.status) && Objects.equals(displayLocation, that.displayLocation)
                && Objects.equals(destination, that.destination) && Objects.equals(when, that.when)
                && Objects.equals(agency, that.agency) && mode == that.mode
                && Objects.equals(platform, that.platform)
                && Objects.equals(callingPoints, that.callingPoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, carriages, status, displayLocation, destination, when, agency, mode, platform, callingPoints);
    }

    @Override
    public String toString() {
        return "UpcomingDeparture{" +
                "date=" + date +
                ", when=" + when +
                ", carriages='" + carriages + '\'' +
                ", status='" + status + '\'' +
                ", displayLocation=" + displayLocation.getId() +
                ", destination=" + destination.getId() +
                ", agency=" + agency.getId() +
                ", mode=" + mode +
                ", callingPoints " + callingPoints +
                ", platform=" + getStringFor() +
                '}';
    }

    private IdFor<Platform> getStringFor() {
        if (platform==null) {
            return IdFor.invalid(Platform.class);
        }
        return platform.getId();
    }


    public boolean hasCallingPoints() {
        return !callingPoints.isEmpty();
    }

    public ImmutableIdSet<Station> getCallingPoints() {
        return callingPoints;
    }
}
