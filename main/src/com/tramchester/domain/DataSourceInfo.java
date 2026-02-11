package com.tramchester.domain;

import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.reference.TransportMode;

import java.time.ZonedDateTime;

public class DataSourceInfo {

    private final DataSourceID sourceID;
    private final String version;
    private final ZonedDateTime lastModTime;
    private final ImmutableEnumSet<TransportMode> modes;

    public DataSourceInfo(DataSourceID sourceID, String version, ZonedDateTime lastModTime, ImmutableEnumSet<TransportMode> modes) {
        this.sourceID = sourceID;
        this.version = version;
        this.lastModTime = lastModTime;
        this.modes = modes;
    }

    /***
     * ONLY Used when gtfs source contains feedinfo file, prefer the version data from there
     * @param dataSourceInfo original feedinfo data
     * @param feedInfo the data from gtfs
     * @return updated version
     */
    public static DataSourceInfo updatedVersion(DataSourceInfo dataSourceInfo, FeedInfo feedInfo) {
        return new DataSourceInfo(dataSourceInfo.sourceID, feedInfo.version(), dataSourceInfo.lastModTime, dataSourceInfo.modes);
    }

    public DataSourceID getID() {
        return sourceID;
    }

    public String getVersion() {
        return version;
    }

    public ZonedDateTime getLastModTime() {
        return lastModTime;
    }

    public ImmutableEnumSet<TransportMode> getModes() {
        return modes;
    }

    @Override
    public String toString() {
        return "DataSourceInfo{" +
                "name='" + sourceID + '\'' +
                ", version='" + version + '\'' +
                ", lastModTime=" + lastModTime +
                ", modes=" + modes +
                '}';
    }
}
