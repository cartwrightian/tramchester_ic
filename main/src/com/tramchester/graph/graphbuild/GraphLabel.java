package com.tramchester.graph.graphbuild;

import com.tramchester.domain.reference.TransportMode;
import org.neo4j.graphdb.Label;

import java.util.EnumSet;
import java.util.stream.Collectors;

import static java.lang.String.format;

public enum GraphLabel implements Label {
    GROUPED,  // grouped station node
    ROUTE_STATION,
    STATION, // any station node must have this label
    HAS_PLATFORMS, // label added to stations iff have platforms
    INTERCHANGE, // label added to route stations if they are interchanges
    // transport modes - added to route stations
    TRAM,
    BUS,
    TRAIN,
    FERRY, // aka a port
    SUBWAY,

    HAS_DIVERSION, // impacted by a diversion

    PLATFORM,
    QUERY_NODE, // created to support journeys walking from arbitary location
    SERVICE,
    HOUR,
    MINUTE,
    // meta labels for versions and feature flags
    VERSION,
    NEIGHBOURS_ENABLED,
    BOUNDS,
    WALK_FOR_CLOSED_ENABLED,
    TEMP_WALKS_ADDED,
    COMPOSITES_ADDED,

    // Order for HOUR_N matters, used in sorting
    HOUR_0, HOUR_1, HOUR_2, HOUR_3, HOUR_4, HOUR_5, HOUR_6, HOUR_7,
    HOUR_8, HOUR_9, HOUR_10, HOUR_11, HOUR_12, HOUR_13, HOUR_14, HOUR_15,
    HOUR_16, HOUR_17, HOUR_18, HOUR_19, HOUR_20, HOUR_21, HOUR_22, HOUR_23;

    private static final GraphLabel[] hourLabels;

    public static final EnumSet<GraphLabel> TransportModes = EnumSet.of(TRAM, BUS, TRAIN, FERRY, SUBWAY);

    static {
        hourLabels = new GraphLabel[24];
        for (int hour = 0; hour < 24; hour++) {
            hourLabels[hour] = GraphLabel.valueOf(format("HOUR_%d", hour));
        }
    }

    public static GraphLabel forMode(final TransportMode mode) {
        return switch (mode) {
            case Tram -> TRAM;
            case Bus -> BUS;
            case Train, RailReplacementBus -> TRAIN;
            case Ferry, Ship -> FERRY;
            case Subway -> SUBWAY;
            case Walk -> QUERY_NODE; // TODO This is inconsistent!
            default -> throw new RuntimeException("Unsupported mode " + mode);
        };
    }

    public static EnumSet<GraphLabel> forModes(final EnumSet<TransportMode> modes) {
        return modes.stream().
                map(GraphLabel::forMode).
                collect(Collectors.toCollection( () -> EnumSet.noneOf(GraphLabel.class)));
    }

    public static Label getHourLabel(final int hour) {
        return hourLabels[hour];
    }

    // TODO performance
    public static int getHourFrom(final EnumSet<GraphLabel> labels) {
        for (int hour = 0; hour < 24 ; hour++) {
            if (labels.contains(hourLabels[hour])) {
                return hour;
            }
        }
        throw new RuntimeException("Could not find hour from " + labels);
    }

    public static EnumSet<GraphLabel> from(final Iterable<Label> iter) {
        // results from perf test, seconds

        // 1.221
        final EnumSet<GraphLabel> result = EnumSet.noneOf(GraphLabel.class);
        for(final Label item : iter) {
            result.add(GraphLabel.valueOf(item.name()));
        }
        return result;

        // 1.284
//        final EnumSet<GraphLabel> result = EnumSet.noneOf(GraphLabel.class);
//        iter.forEach(item -> result.add(GraphLabel.valueOf(item.name())));
//        return result;

        // 1.688
        // return Streams.stream(iter).map(label -> GraphLabel.valueOf(label.name())).collect(Collectors.toCollection(() -> EnumSet.noneOf(GraphLabel.class)));

        // 3.849
        //  final Set<GraphLabel> set = Streams.stream(iter).map(label -> GraphLabel.valueOf(label.name())).collect(Collectors.toSet());
        //  return EnumSet.copyOf(set);
    }
}
