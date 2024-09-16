package com.tramchester.unit.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.dataimport.GetsFileModTime;
import com.tramchester.dataimport.HttpDownloadAndModTime;
import com.tramchester.dataimport.S3DownloadAndModTime;
import com.tramchester.dataimport.URLStatus;
import com.tramchester.domain.ServiceTimeLimits;
import com.tramchester.healthchecks.NewDataAvailableHealthCheck;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.time.ZonedDateTime;

class NewDataAvailableHealthCheckTest extends EasyMockSupport {
    private URI expecteds3URI;
    private URI expectedURL;

    private HttpDownloadAndModTime httpDownloadAndModTime;
    private GetsFileModTime getsFileModTime;
    private NewDataAvailableHealthCheck healthCheck;
    private ZonedDateTime time;
    private RemoteDataSourceConfig dataSourceConfig;
    private S3DownloadAndModTime s3DownloadAndModTime;

    @BeforeEach
    void beforeEachTestRuns() {
        dataSourceConfig = createMock(RemoteDataSourceConfig.class);

        httpDownloadAndModTime = createStrictMock(HttpDownloadAndModTime.class);
        s3DownloadAndModTime = createStrictMock(S3DownloadAndModTime.class);
        getsFileModTime = createMock(GetsFileModTime.class);
        expectedURL = URI.create("http://somedata.source.com/path");
        expecteds3URI = URI.create("s3://tramchesternewdist/dist/642/tfgm_data.zip");
        ServiceTimeLimits serviceTimeLimits = new ServiceTimeLimits();

        healthCheck = new NewDataAvailableHealthCheck(dataSourceConfig, httpDownloadAndModTime, s3DownloadAndModTime,
                getsFileModTime, serviceTimeLimits);
        time = TestEnv.UTCNow();
    }

    @Test
    void shouldReportHealthyWhenNONewDataAvailable() throws IOException, InterruptedException {

        EasyMock.expect(dataSourceConfig.getDataCheckUrl()).andReturn(expectedURL.toString());
        EasyMock.expect(dataSourceConfig.isMandatory()).andReturn(true);

        URLStatus status = new URLStatus(expectedURL, 200, time.minusDays(1));

        EasyMock.expect(httpDownloadAndModTime.getStatusFor(expectedURL, time, true)).andReturn(status);
        EasyMock.expect(getsFileModTime.getFor(dataSourceConfig)).andReturn(time);

        replayAll();
        HealthCheck.Result result = healthCheck.execute();
        Assertions.assertTrue(result.isHealthy());
        verifyAll();
    }

    @Test
    void shouldReportUnHealthyWhenNewDataAvailable() throws IOException, InterruptedException {

        EasyMock.expect(dataSourceConfig.getDataCheckUrl()).andReturn(expectedURL.toString());
        EasyMock.expect(dataSourceConfig.isMandatory()).andReturn(true);

        URLStatus status = new URLStatus(expectedURL, 200, time.plusDays(1));

        EasyMock.expect(httpDownloadAndModTime.getStatusFor(expectedURL, time, true)).andReturn(status);
        EasyMock.expect(getsFileModTime.getFor(dataSourceConfig)).andReturn(time);

        replayAll();
        HealthCheck.Result result = healthCheck.execute();
        Assertions.assertFalse(result.isHealthy());
        verifyAll();
    }

    @Test
    void shouldReportUnHealthyWhenDataMissing() throws IOException, InterruptedException {

        EasyMock.expect(dataSourceConfig.getDataCheckUrl()).andReturn(expectedURL.toString());
        EasyMock.expect(dataSourceConfig.isMandatory()).andReturn(true);

        URLStatus status = new URLStatus(expectedURL, 200);

        EasyMock.expect(httpDownloadAndModTime.getStatusFor(expectedURL, time, true)).andReturn(status);
        EasyMock.expect(getsFileModTime.getFor(dataSourceConfig)).andReturn(time);

        replayAll();
        HealthCheck.Result result = healthCheck.execute();
        Assertions.assertFalse(result.isHealthy());
        verifyAll();
    }

    @Test
    void shouldReportHealthyWhenNONewDataAvailableS3() {

        EasyMock.expect(dataSourceConfig.getDataCheckUrl()).andReturn(expecteds3URI.toString());
        EasyMock.expect(dataSourceConfig.isMandatory()).andReturn(true);

        URLStatus status = new URLStatus(expecteds3URI, 200, time.minusDays(1));

        EasyMock.expect(s3DownloadAndModTime.getStatusFor(expecteds3URI, time, true)).andReturn(status);
        EasyMock.expect(getsFileModTime.getFor(dataSourceConfig)).andReturn(time);

        replayAll();
        HealthCheck.Result result = healthCheck.execute();
        Assertions.assertTrue(result.isHealthy());
        verifyAll();
    }

    @Test
    void shouldReportUnHealthyWhenNewDataAvailableS3() {

        EasyMock.expect(dataSourceConfig.getDataCheckUrl()).andReturn(expecteds3URI.toString());
        EasyMock.expect(dataSourceConfig.isMandatory()).andReturn(true);

        URLStatus status = new URLStatus(expectedURL, 200, time.plusDays(1));

        EasyMock.expect(s3DownloadAndModTime.getStatusFor(expecteds3URI, time, true)).andReturn(status);
        EasyMock.expect(getsFileModTime.getFor(dataSourceConfig)).andReturn(time);

        replayAll();
        HealthCheck.Result result = healthCheck.execute();
        Assertions.assertFalse(result.isHealthy());
        verifyAll();
    }

    @Test
    void shouldReportUnHealthyWhenDataMissingS3() {

        EasyMock.expect(dataSourceConfig.getDataCheckUrl()).andReturn(expecteds3URI.toString());
        EasyMock.expect(dataSourceConfig.isMandatory()).andReturn(true);

        URLStatus status = new URLStatus(expectedURL, 200);

        EasyMock.expect(s3DownloadAndModTime.getStatusFor(expecteds3URI, time, true)).andReturn(status);
        EasyMock.expect(getsFileModTime.getFor(dataSourceConfig)).andReturn(time);

        replayAll();
        HealthCheck.Result result = healthCheck.execute();
        Assertions.assertFalse(result.isHealthy());
        verifyAll();
    }

}
