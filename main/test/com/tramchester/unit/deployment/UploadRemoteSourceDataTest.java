package com.tramchester.unit.deployment;

import com.tramchester.cloud.data.UploadFileToS3;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.RemoteDataAvailable;
import com.tramchester.deployment.UploadRemoteSourceData;
import com.tramchester.domain.DataSourceID;
import com.tramchester.testSupport.TestConfig;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class UploadRemoteSourceDataTest extends EasyMockSupport {

    private UploadFileToS3 s3Uploader;
    private UploadRemoteSourceData uploadRemoteData;
    private RemoteDataAvailable dataRefreshed;

    @BeforeEach
    void beforeEachTestRuns() {

        s3Uploader = createStrictMock(UploadFileToS3.class);
        dataRefreshed = createMock(RemoteDataAvailable.class);

        List<RemoteDataSourceConfig> remoteConfigs = new ArrayList<>();

        remoteConfigs.add(new DataSourceConfig(Path.of("data/xxx"), "filenameA.zip", DataSourceID.tfgm));
        remoteConfigs.add(new DataSourceConfig(Path.of("data/yyy"), "filenameB.txt", DataSourceID.openRailData));
        remoteConfigs.add(new DataSourceConfig(Path.of("data/zzz"), "filenameC.zip", DataSourceID.nptg, "filenameC.xml"));
        remoteConfigs.add(new DataSourceConfig(Path.of("data/abc"), "filenameD.xml", DataSourceID.naptanxml));

        TramchesterConfig config = new ConfigWithRemoteSource(remoteConfigs);

        uploadRemoteData = new UploadRemoteSourceData(s3Uploader, config, dataRefreshed);
    }

    @Test
    void shouldUpdateEachRemoteDataSourceInConfigToS3() {

        EasyMock.expect(dataRefreshed.hasFileFor(DataSourceID.tfgm)).andReturn(true);
        EasyMock.expect(dataRefreshed.hasFileFor(DataSourceID.openRailData)).andReturn(true);
        EasyMock.expect(dataRefreshed.hasFileFor(DataSourceID.nptg)).andReturn(true);
        EasyMock.expect(dataRefreshed.hasFileFor(DataSourceID.naptanxml)).andReturn(false);


        final String prefix = "aPrefix";
        EasyMock.expect(s3Uploader.uploadFile(prefix, Path.of("data/xxx/filenameA.zip"), true)).andReturn(true);
        EasyMock.expect(s3Uploader.uploadFile(prefix, Path.of("data/yyy/filenameB.txt"), true)).andReturn(true);

        EasyMock.expect(s3Uploader.uploadFileZipped(prefix, Path.of("data/zzz/filenameC.xml"), true, "filenameC.zip")).andReturn(true);

        replayAll();
        boolean result = uploadRemoteData.upload(prefix);
        verifyAll();

        assertTrue(result);
    }

    @Test
    void shouldUploadFileAsCompressedWithZipPostfix() {

        EasyMock.expect(dataRefreshed.hasFileFor(DataSourceID.tfgm)).andReturn(false);
        EasyMock.expect(dataRefreshed.hasFileFor(DataSourceID.openRailData)).andReturn(false);
        EasyMock.expect(dataRefreshed.hasFileFor(DataSourceID.nptg)).andReturn(false);
        EasyMock.expect(dataRefreshed.hasFileFor(DataSourceID.naptanxml)).andReturn(true);


        final String prefix = "aPrefix";

        EasyMock.expect(s3Uploader.uploadFileZipped(prefix, Path.of("data/abc/filenameD.xml"), true, "filenameD.xml.zip")).andReturn(true);

        replayAll();
        boolean result = uploadRemoteData.upload(prefix);
        verifyAll();

        assertTrue(result);
    }

    @Test
    void shouldFailIfAnyFail() {

        EasyMock.expect(dataRefreshed.hasFileFor(DataSourceID.tfgm)).andReturn(true);
        EasyMock.expect(dataRefreshed.hasFileFor(DataSourceID.openRailData)).andReturn(true);
        EasyMock.expect(dataRefreshed.hasFileFor(DataSourceID.nptg)).andReturn(true);
        EasyMock.expect(dataRefreshed.hasFileFor(DataSourceID.naptanxml)).andReturn(false);

        final String prefix = "somePrefix";
        EasyMock.expect(s3Uploader.uploadFile(prefix, Path.of("data/xxx/filenameA.zip"), true)).andReturn(true);
        EasyMock.expect(s3Uploader.uploadFile(prefix, Path.of("data/yyy/filenameB.txt"), true)).andReturn(false);

        replayAll();
        boolean result = uploadRemoteData.upload(prefix);
        verifyAll();

        assertFalse(result);
    }

    private static class ConfigWithRemoteSource extends TestConfig {
        private final List<RemoteDataSourceConfig> remoteConfigs;

        private ConfigWithRemoteSource(List<RemoteDataSourceConfig> remoteConfigs) {
            this.remoteConfigs = remoteConfigs;
        }

        @Override
        protected List<GTFSSourceConfig> getDataSourceFORTESTING() {
            return null;
        }

        @Override
        public List<RemoteDataSourceConfig> getRemoteDataSourceConfig() {
            return remoteConfigs;
        }

        @Override
        public String getDistributionBucket() {
            return "bucket";
        }
    }

    private static class DataSourceConfig extends RemoteDataSourceConfig {
        private final Path path;
        private final String filename;
        private final DataSourceID dataSourceID;
        private final String modCheckFilename;

        private DataSourceConfig(Path path, String filename, DataSourceID dataSourceID) {
            this(path, filename, dataSourceID, "");
        }

        private DataSourceConfig(Path path, String filename, DataSourceID dataSourceID, String modCheckFilename) {
            this.path = path;
            this.filename = filename;
            this.dataSourceID = dataSourceID;
            this.modCheckFilename = modCheckFilename;
        }

        @Override
        public Path getDataPath() {
            return path;
        }

        @Override
        public Path getDownloadPath() {
            return getDataPath();
        }

        @Override
        public String getDataCheckUrl() {
            return "";
        }

        @Override
        public String getDataUrl() {
            return "";
        }

        @Override
        public boolean checkOnlyIfExpired() {
            return false;
        }

        @Override
        public Duration getDefaultExpiry() {
            return Duration.ofDays(1);
        }

        @Override
        public String getDownloadFilename() {
            return filename;
        }

        @Override
        public String getName() {
            return dataSourceID.name();
        }

        @Override
        public String getModTimeCheckFilename() {
            return modCheckFilename;
        }
    }
}
