package com.tramchester.domain.id;

import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.places.PostcodeLocation;

public class PostcodeLocationId extends ContainsId<PostcodeLocation> {

    private PostcodeLocationId(String text) {
        super(StringIdFor.createId(text, PostcodeLocation.class));
    }

    public static PostcodeLocationId create(String text) {
        return new PostcodeLocationId(text.toUpperCase());
    }

//    @Override
//    public String getGraphId() {
//        return containedId.getGraphId();
//    }

    @Override
    public boolean isValid() {
        return true;
    }

//    @Override
//    public Class<PostcodeLocation> getDomainType() {
//        return PostcodeLocation.class;
//    }

    public String getName() {
        return getContainedId().getContainedId();
    }

    @Override
    public String toString() {
        return "PostcodeLocationId{" +
                "containedId=" + getContainedId() +
                "}";
    }

//    @Override
//    StringIdFor<PostcodeLocation> getContainedId() {
//        return containedId;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        PostcodeLocationId that = (PostcodeLocationId) o;
//        return containedId.equals(that.containedId);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(containedId);
//    }

    public LocationId<PostcodeLocation> getLocationId() {
        // TODO to field? Efficiency.
        return LocationId.wrap(getContainedId());
    }
}
