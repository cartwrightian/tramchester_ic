package com.tramchester.domain;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Walk;

public class Journey implements Iterable<TransportStage<?,?>> {

    private final TramTime queryTime;
    private final TramTime arrivalTime;
    private final TramTime departTime;
    private final int requestedNumberChanges;
    private final List<TransportStage<?,?>> stages;
    private final List<Location<?>> path;
    private final int journeyIndex;

    public Journey(TramTime departTime, TramTime queryTime, TramTime arrivalTime, List<TransportStage<?, ?>> stages,
                   List<Location<?>> path, int requestedNumberChanges, int journeyIndex) {
        this.stages = stages;
        this.queryTime = queryTime;
        this.path = path;
        this.departTime = departTime;
        this.arrivalTime = arrivalTime;
        this.requestedNumberChanges = requestedNumberChanges;
        this.journeyIndex = journeyIndex;
    }
    
    public @NotNull Iterator<TransportStage<?,?>> iterator() {
        return stages.iterator();
    }

    public List<TransportStage<?,?>> getStages() {
        return stages;
    }

    public IdSet<Platform> getCallingPlatformIds() {
       return stages.stream().filter(TransportStage::hasBoardingPlatform).
               map(TransportStage::getBoardingPlatform).
               collect(IdSet.collector());
    }

    public TramTime getQueryTime() {
        return queryTime;
    }

    @Override
    public String toString() {
        return "Journey{" +
                "queryTime=" + queryTime +
                ", arrivalTime=" + arrivalTime +
                ", departTime=" + departTime +
                ", requestedNumberChanges=" + requestedNumberChanges +
                ", stages=" + stages +
                ", path=" + HasId.asIds(path) +
                '}';
    }

    public List<Location<?>> getPath() {
        return path;
    }

    public Set<TransportMode> getTransportModes() {
        return stages.stream().map(TransportStage::getMode).collect(Collectors.toSet());
    }

    public TramTime getArrivalTime() {
        return arrivalTime;
    }

    public TramTime getDepartTime() {
        return departTime;
    }

    public boolean isDirect() {
        int size = stages.size();

        if (size == 1) {
            return true;
        }
        if (firstStageIsWalk() && size == 2) {
            return true;
        }
        return false;
    }

    public boolean firstStageIsWalk() {
        return getFirstStageMode()== Walk;
    }

    public boolean firstStageIsConnect() {
        return getFirstStageMode() ==TransportMode.Connect;
    }

    private TransportMode getFirstStageMode() {
        return stages.get(0).getMode();
    }

    public Location<?> getBeginning() {
        if (firstStageIsWalk()) {
            // TODO Check if this workaround still needed or used?
            if (stages.size()>1) {
                return stages.get(1).getFirstStation();
            }
        }
        return stages.get(0).getFirstStation();
    }

    public Location<?> getDestination() {
       int lastIndex = stages.size() -1;
       if (lastIndex<0) {
           lastIndex = 0;
       }
       return stages.get(lastIndex).getLastStation();
    }

    public int getRequestedNumberChanges() {
        return requestedNumberChanges;
    }

    public List<Location<?>> getChangeStations() {
        // count any change of transport mode as a change station
        if (isDirect()) {
            TransportStage<?, ?> firstStage = stages.get(0);

            final Location<?> changeStation;
            if (firstStage.getMode() == Walk) {
                // walking stage, either to/from a location - we want the actual station here
                if (firstStage.getFirstStation().getLocationType()==LocationType.Station) {
                    changeStation = firstStage.getFirstStation();
                } else {
                    changeStation = firstStage.getLastStation();
                }
                return Collections.singletonList(changeStation);
            } else {
                return Collections.emptyList();
            }
        }

        List<Location<?>> result = new ArrayList<>();

        for(int index = 1; index < stages.size(); index++) {
            result.add(stages.get(index).getFirstStation());
        }

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Journey that = (Journey) o;

        if (!queryTime.equals(that.queryTime)) return false;

        if (requestedNumberChanges != that.requestedNumberChanges) return false;
        if (!arrivalTime.equals(that.arrivalTime)) return false;
        if (!departTime.equals(that.departTime)) return false;
        return stages.equals(that.stages);
    }

    @Override
    public int hashCode() {
        int result = queryTime.hashCode();
        result = 31 * result + arrivalTime.hashCode();
        result = 31 * result + departTime.hashCode();
        result = 31 * result + requestedNumberChanges;
        result = 31 * result + stages.hashCode();
        return result;
    }

    public int getJourneyIndex() {
        return journeyIndex;
    }

}
