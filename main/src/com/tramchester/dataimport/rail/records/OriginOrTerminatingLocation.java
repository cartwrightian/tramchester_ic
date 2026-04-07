package com.tramchester.dataimport.rail.records;

import com.tramchester.dataimport.rail.records.reference.LocationActivityCode;
import com.tramchester.domain.collections.ImmutableEnumSet;
import com.tramchester.domain.time.TramTime;
import org.apache.commons.lang3.NotImplementedException;

public abstract class OriginOrTerminatingLocation {

    private final String tiplocCode;
    private final TramTime publicTime;
    private final String platform;
    private final ImmutableEnumSet<LocationActivityCode> activity;

    protected OriginOrTerminatingLocation(String tiplocCode, TramTime publicTime, String platform, ImmutableEnumSet<LocationActivityCode> activity) {
        this.tiplocCode = tiplocCode;
        this.publicTime = publicTime;
        this.platform = platform;
        this.activity = activity;
    }

    protected static <T extends OriginOrTerminatingLocation> T parse(final String text, final Constructor<T> builder,
                                                                     final RecordHelper recordHelper) {
        // NOTE: for terminating and originating locations a suffix is added and docs give total length as 8
        // but this causes stations not to be found, so use length of 7 here
        final String tiplocCode = recordHelper.extract(text, 3, 10);
        final TramTime tramTime = recordHelper.extractTime(text, 15);
        final String platform = recordHelper.extract(text, 20, 22+1).trim();
        return builder.create(tiplocCode, tramTime, platform);
    }

    public String getTiplocCode() {
        return tiplocCode;
    }

    protected TramTime getPublicTime() {
        return publicTime;
    }

    public String getPlatform() {
        return platform;
    }

    protected interface Constructor<T> {
        T create(String tiplocCode, TramTime tramTime, String platform);
    }

    @Override
    public String toString() {
        return "OriginOrTerminatingLocation{" +
                "tiplocCode='" + tiplocCode + '\'' +
                ", publicTime=" + publicTime +
                ", platform='" + platform + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OriginOrTerminatingLocation that = (OriginOrTerminatingLocation) o;

        if (!tiplocCode.equals(that.tiplocCode)) return false;
        if (!publicTime.equals(that.publicTime)) return false;
        return platform.equals(that.platform);
    }

    @Override
    public int hashCode() {
        int result = tiplocCode.hashCode();
        result = 31 * result + publicTime.hashCode();
        result = 31 * result + platform.hashCode();
        return result;
    }

    public TramTime getPassingTime() {
        throw new NotImplementedException("Not implemented, record was " + this);
    }

    public ImmutableEnumSet<LocationActivityCode> getActivity() {
        return activity;
    }

    public boolean doesStop() {
        return LocationActivityCode.doesStop(activity);
    }


}
