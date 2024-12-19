package com.tramchester.domain;

import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.DTO.diagnostics.JourneyDiagnostics;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TimeRangePartial;
import com.tramchester.domain.time.TramTime;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class JourneyRequest {
    private final TramDate date;
    private final TramTime originalQueryTime;
    private final boolean arriveBy;
    private final MaxNumberOfChanges maxChanges;
    private final UUID uid;
    private final Duration maxJourneyDuration;
    private final long maxNumberOfJourneys;

    private final EnumSet<TransportMode> requestedModes;

    private boolean diagRequested;
    private boolean warnIfNoResults;
    private boolean cachingDisabled;

    private final AtomicBoolean diagnosticsReceived;
    private JourneyDiagnostics journeyDiagnostics;

    @Deprecated
    public JourneyRequest(TramDate date, TramTime originalQueryTime, boolean arriveBy, int maxChanges,
                          Duration maxJourneyDuration, long maxNumberOfJourneys, EnumSet<TransportMode> requestedModes) {
        this(date, originalQueryTime, arriveBy, new MaxNumberOfChanges(maxChanges), maxJourneyDuration, maxNumberOfJourneys, requestedModes);
    }

    public JourneyRequest(TramDate date, TramTime originalQueryTime, boolean arriveBy, MaxNumberOfChanges maxChanges,
                          Duration maxJourneyDuration, long maxNumberOfJourneys, EnumSet<TransportMode> requestedModes) {
        this.date = date;
        this.originalQueryTime = originalQueryTime;
        this.arriveBy = arriveBy;
        this.maxChanges = maxChanges;
        this.maxJourneyDuration = maxJourneyDuration;
        this.maxNumberOfJourneys = maxNumberOfJourneys;
        this.uid = UUID.randomUUID();
        this.requestedModes = requestedModes;

        if (requestedModes.isEmpty()) {
            throw new RuntimeException("Must provides modes");
        }

        diagRequested = false;
        cachingDisabled = false;
        warnIfNoResults = true;
        diagnosticsReceived = new AtomicBoolean(false);
    }

    public JourneyRequest(JourneyRequest originalRequest, TramTime computedDepartTime) {
        this(originalRequest.date, computedDepartTime, originalRequest.arriveBy, originalRequest.maxChanges,
                originalRequest.maxJourneyDuration, originalRequest.maxNumberOfJourneys, originalRequest.requestedModes);
        diagRequested = originalRequest.diagRequested;
        warnIfNoResults = originalRequest.warnIfNoResults;
    }

    public TramDate getDate() {
        return date;
    }

    public TramTime getOriginalTime() {
        return originalQueryTime;
    }

    public boolean getArriveBy() {
        return arriveBy;
    }

    public MaxNumberOfChanges getMaxChanges() {
        return maxChanges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JourneyRequest that = (JourneyRequest) o;

        if (arriveBy != that.arriveBy) return false;
        if (maxChanges != that.maxChanges) return false;
        if (maxNumberOfJourneys != that.maxNumberOfJourneys) return false;
        if (!date.equals(that.date)) return false;
        if (!originalQueryTime.equals(that.originalQueryTime)) return false;
        return maxJourneyDuration.equals(that.maxJourneyDuration);
    }

    @Override
    public int hashCode() {
        int result = date.hashCode();
        result = 31 * result + originalQueryTime.hashCode();
        result = 31 * result + (arriveBy ? 1 : 0);
        result = 31 * result + maxChanges.hashCode();
        result = 31 * result + maxJourneyDuration.hashCode();
        result = 31 * result + (int) (maxNumberOfJourneys ^ (maxNumberOfJourneys >>> 32));
        return result;
    }

    public boolean getDiagnosticsEnabled() {
        return diagRequested;
    }

    @SuppressWarnings("unused")
    public JourneyRequest setDiag(boolean flag) {
        diagRequested = flag;
        return this;
    }

    @SuppressWarnings("unused")
    public void setCachingDisabled(boolean flag) {
        // aids with debugging and performance tuning
        this.cachingDisabled = flag;
    }

    public Duration getMaxJourneyDuration() {
        return maxJourneyDuration;
    }

    public boolean getWarnIfNoResults() {
        return warnIfNoResults;
    }

    public void setWarnIfNoResults(boolean flag) {
        warnIfNoResults = flag;
    }

    public UUID getUid() {
        return uid;
    }

    public long getMaxNumberOfJourneys() {
        return maxNumberOfJourneys;
    }

    @Override
    public String toString() {
        return "JourneyRequest{" +
                "date=" + date +
                ", originalQueryTime=" + originalQueryTime +
                ", arriveBy=" + arriveBy +
                ", maxChanges=" + maxChanges +
                ", uid=" + uid +
                ", maxJourneyDuration=" + maxJourneyDuration +
                ", maxNumberOfJourneys=" + maxNumberOfJourneys +
                ", allowedModes=" + requestedModes +
                '}';
    }

    public EnumSet<TransportMode> getRequestedModes() {
        return requestedModes;
    }

    public TimeRange getTimeRange() {
        return TimeRangePartial.of(originalQueryTime, Duration.ZERO, maxJourneyDuration);
    }

    public synchronized void injectDiag(final JourneyDiagnostics diagnostics) {
        // todo need better way to handle passing back diagnostics
        // for now when diags is enabled don't look for range of times or changes
        if (diagnosticsReceived.get()) {
            throw new RuntimeException("Already set");
        }
        this.journeyDiagnostics = diagnostics;
        diagnosticsReceived.set(true);
    }

    public boolean hasReceivedDiagnostics() {
        return diagnosticsReceived.get();
    }

    public JourneyDiagnostics getDiagnostics() {
        return journeyDiagnostics;
    }

    public boolean getCachingDisabled() {
        return cachingDisabled;
    }

    public static class MaxNumberOfChanges {

        private final int maxChanges;

        public MaxNumberOfChanges(int maxChanges) {

            this.maxChanges = maxChanges;
        }

        public static MaxNumberOfChanges of(int number) {
            return new MaxNumberOfChanges(number);
        }

        public int get() {
            if (maxChanges<0) {
                throw new RuntimeException("Called for " + maxChanges);
            }
            return maxChanges;
        }

        public boolean isUseComputed() {
            return maxChanges==-1;
        }

        @Override
        public String toString() {
            if (maxChanges==-1) {
                return "MaxNumberOfChanges{" +
                        "maxChanges=USE_COMPUTED}";
            }
            return "MaxNumberOfChanges{" +
                    "maxChanges=" + maxChanges +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MaxNumberOfChanges that)) return false;
            return maxChanges == that.maxChanges;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(maxChanges);
        }
    }


}
