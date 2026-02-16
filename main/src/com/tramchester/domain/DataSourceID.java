package com.tramchester.domain;

import com.tramchester.graph.GraphPropertyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

public enum DataSourceID {
    internal, // for walks, MyLocation, etc
    tfgm, // transport for greater manchester
    nptg, // National Public Transport Gazetteer, locations data
    postcode,
    naptanxml, // Naptan (stops) data in xml form, cross references nptg
//    @Deprecated
//    rail, // AToC rail timetable data AFTER has been downloaded
    database, // pre-built graph db for use during deployment
    openRailData,
    unknown;

    private static final Logger logger = LoggerFactory.getLogger(DataSourceID.class);

    public static EnumSet<DataSourceID> InDatabase() {
        return EnumSet.of(naptanxml, nptg, tfgm, openRailData, postcode);
    }

    public static DataSourceID findOrUnknown(final String name) {
        try {
            return valueOf(name);
        }
        catch (IllegalArgumentException exception) {
            // TODO Rethrow as Runtime ??
            logger.error("Unknown DataSourceId " + name, exception);
            return unknown;
        }
    }

    public static GraphPropertyKey getGraphKey(final DataSourceID dataSourceID) {
        return switch (dataSourceID) {
            case nptg -> GraphPropertyKey.NPTG_VERSION;
            case tfgm -> GraphPropertyKey.TFGM_VERSION;
            case postcode -> GraphPropertyKey.POSTCODE_VERSION;
            case naptanxml -> GraphPropertyKey.NAPTAN_VERSION;
            case openRailData -> GraphPropertyKey.OPENRAILDATA_VERSION;
            default -> throw new RuntimeException("Cannot map " + dataSourceID + " to a key");
        };
    }
}
