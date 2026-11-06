package com.tramchester.domain.places;

public enum InterchangeType {
    FromSourceData,     // flagged as interchange in the source data
    NumberOfLinks,      // auto discovered due to number routes
    NeighbourLinksSingleMode,     // added as neighbour to another station
    NeighbourLinksMultimodal,
    Multimodal,         // multi transport station from source data
    FromConfig          // from config
}
