package com.tramchester.domain.id;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;

public class PlatformId extends ContainsId<Platform> {
    private final String platformNumber;

    private PlatformId(IdFor<Station> stationId, String platformNumber) {
        super(StringIdFor.concat(stationId, platformNumber, Platform.class));
        this.platformNumber = platformNumber;
    }

    public static PlatformId createId(Station station, String platformNumber) {
        return new PlatformId(station.getId(), platformNumber);
    }

    public static PlatformId createId(IdFor<Station> stationId, String platformNumber) {
        return new PlatformId(stationId, platformNumber);
    }

    public static <FROM extends CoreDomain,TO extends CoreDomain> IdFor<TO> convert(IdFor<FROM> original, Class<TO> domainType) {
        guardForType(original);
        PlatformId originalPlatformId = (PlatformId) original;
        return StringIdFor.convert(originalPlatformId.getContainedId(), domainType);
    }

    private static <FROM extends CoreDomain> void guardForType(IdFor<FROM> original) {
        if (!(original instanceof PlatformId)) {
            throw new RuntimeException(original + " is not a PlatformId");
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public String getNumber() {
        return platformNumber;
    }

    @Override
    public String toString() {
        return "PlatformId{" +
                "platformNumber='" + platformNumber + '\'' +
                ", containedId=" + getContainedId().getContainedId() +
                "} ";
    }
}
