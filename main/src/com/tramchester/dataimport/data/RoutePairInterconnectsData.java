package com.tramchester.dataimport.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.caching.CachableData;
import com.tramchester.mappers.serialisation.BitSetDeserializer;
import com.tramchester.mappers.serialisation.BitSetSerializer;

import java.util.BitSet;

@JsonIgnoreProperties(ignoreUnknown = false)
public class RoutePairInterconnectsData implements CachableData {
    private final int depth;
    private final short routeA;
    private final short routeB;

    @JsonDeserialize(using = BitSetDeserializer.class)
    @JsonSerialize(using = BitSetSerializer.class)
    private final BitSet overlaps;

    @JsonCreator
    public RoutePairInterconnectsData(
            @JsonProperty("depth") int depth,
            @JsonProperty("routeA") short routeA,
            @JsonProperty("routeB") short routeB,
            @JsonProperty("overlaps") BitSet overlaps) {

        this.depth = depth;
        this.routeA = routeA;
        this.routeB = routeB;
        this.overlaps = overlaps;
    }

    public int getDepth() {
        return depth;
    }

    public BitSet getOverlaps() {
        return overlaps;
    }

    public short getRouteA() {
        return routeA;
    }

    public short getRouteB() {
        return routeB;
    }
}
