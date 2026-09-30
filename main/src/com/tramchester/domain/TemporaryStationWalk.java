package com.tramchester.domain;

import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramDuration;
import com.tramchester.mappers.Geography;

import java.util.Objects;

public class TemporaryStationWalk {
    private final StationPair stationPair;
    private final DateRange dateRange;
    private final DataSourceID dataSourceID;
    private final TramDuration costOverride;

    public TemporaryStationWalk(StationPair stationPair, DateRange dateRange, DataSourceID dataSourceID) {
        this(stationPair, dateRange, dataSourceID, null);
    }

    public TemporaryStationWalk(StationPair stationPair, DateRange dateRange, DataSourceID dataSourceID,
                                TramDuration costOverride) {
        this.stationPair = stationPair;
        this.dateRange = dateRange;
        this.dataSourceID = dataSourceID;
        this.costOverride = costOverride;

    }

    public StationPair getStationPair() {
        return stationPair;
    }

    public DateRange getDateRange() {
        return dateRange;
    }

    public TramDuration getCost(final Geography geography) {
        if (costOverride==null) {
            return geography.getWalkingDuration(stationPair.getBegin(), stationPair.getEnd());
        }
        return costOverride;
    }

    // Right now assumed these are always for a whole day
    public TimeRange getTimeRange() {
        return TimeRange.AllDay();
    }

    public DataSourceID getDataSourceID() {
        return dataSourceID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemporaryStationWalk that = (TemporaryStationWalk) o;
        return Objects.equals(getStationPair(), that.getStationPair()) && Objects.equals(getDateRange(), that.getDateRange()) && getDataSourceID() == that.getDataSourceID();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStationPair(), getDateRange(), getDataSourceID());
    }

    @Override
    public String toString() {
        return "TemporaryStationWalk{" +
                "stationPair=" + HasId.asIds(stationPair) +
                ", dateRange=" + dateRange +
                ", dataSourceID=" + dataSourceID +
                '}';
    }

}
