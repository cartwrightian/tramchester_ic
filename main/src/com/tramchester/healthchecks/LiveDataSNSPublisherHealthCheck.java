package com.tramchester.healthchecks;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.ServiceTimeLimits;
import com.tramchester.livedata.tfgm.LiveDataSNSPublisher;

import jakarta.inject.Inject;

@LazySingleton
public class LiveDataSNSPublisherHealthCheck extends TramchesterHealthCheck {
    private final LiveDataSNSPublisher liveDataSNSPublisher;

    @Inject
    public LiveDataSNSPublisherHealthCheck(ServiceTimeLimits serviceTimeLimits, LiveDataSNSPublisher liveDataSNSPublisher) {
        super(serviceTimeLimits);
        this.liveDataSNSPublisher = liveDataSNSPublisher;
    }

    @Override
    public String getName() {
        return "LiveDataSNSPublish";
    }

    @Override
    public boolean isEnabled() {
        return liveDataSNSPublisher.isEnabled();
    }

    @Override
    protected Result check() throws Exception {
        if (liveDataSNSPublisher.getLastSentOk()) {
            return Result.healthy("Last message send ok");
        } else {
            return Result.unhealthy("Did not send last message successfully");
        }
    }
}
