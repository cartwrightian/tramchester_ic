package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.tramchester.domain.BoundingBoxWithCost;
import com.tramchester.domain.Journey;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.mappers.JourneyToDTOMapper;

@SuppressWarnings("unused")
@JsonTypeName("BoxWithCost")
@JsonTypeInfo(include=JsonTypeInfo.As.WRAPPER_OBJECT, use=JsonTypeInfo.Id.NAME)
public class BoxWithCostDTO {

    private LatLong bottomLeft;
    private LatLong topRight;
    private long minutes;
    private JourneyDTO journey;

    private BoxWithCostDTO(LatLong bottomLeft, LatLong topRight, long minutes, JourneyDTO journey) {
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
        this.minutes = minutes;
        this.journey = journey;
    }

    public BoxWithCostDTO() {
        // deserialisation
    }

    public static BoxWithCostDTO createFrom(final JourneyToDTOMapper mapper, final TramDate serviceDate,
                                            final BoundingBoxWithCost box) {

        // TODO Assuming valid positions here
        final LatLong bottomLeft = CoordinateTransforms.getLatLong(box.getBottomLeft());
        final LatLong topRight = CoordinateTransforms.getLatLong(box.getTopRight());

        final long mins = box.getDuration().toMinutes();

        final Journey journey = box.getJourney();
        if (journey !=null) {
            return new BoxWithCostDTO(bottomLeft, topRight, mins, mapper.createJourneyDTO(journey, serviceDate));
        } else {
            return new BoxWithCostDTO(bottomLeft, topRight, mins, null);
        }
    }

    public long getMinutes() {
        return minutes;
    }

    public LatLong getBottomLeft() {
        return bottomLeft;
    }

    public LatLong getTopRight() {
        return topRight;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public JourneyDTO getJourney() {
        return journey;
    }

    @Override
    public String toString() {
        return "BoxWithCostDTO{" +
                "bottomLeft=" + bottomLeft +
                ", topRight=" + topRight +
                ", minutes=" + minutes +
                ", journey=" + journey +
                '}';
    }
}
