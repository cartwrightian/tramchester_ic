package com.tramchester.domain.id;

import com.tramchester.domain.places.LocationId;
import com.tramchester.domain.places.PostcodeLocation;

public class PostcodeLocationId extends ContainsId<PostcodeLocation> {

    private final String name;

    private PostcodeLocationId(final String text) {
        super(StringIdFor.createId(text, PostcodeLocation.class));
        this.name = text;
    }

    public static PostcodeLocationId create(final String text) {
        return new PostcodeLocationId(text.toUpperCase());
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public String getName() {
        return name;
        //return getContainedId().getContainedId();
    }

    @Override
    public String toString() {
        return "PostcodeLocationId{" +
                "containedId=" + getContainedId() +
                "name=" + name +
                "}";
    }

    public LocationId<PostcodeLocation> getLocationId() {
        // TODO to field? Efficiency.
        return LocationId.wrap(getContainedId());
    }
}
