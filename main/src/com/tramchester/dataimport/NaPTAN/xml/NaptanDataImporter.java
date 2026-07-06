package com.tramchester.dataimport.NaPTAN.xml;

import com.google.inject.ImplementedBy;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanStopData;
import com.tramchester.dataimport.RemoteDataAvailable;
import com.tramchester.dataimport.loader.files.ElementsFromXMLFile;
import com.tramchester.domain.DataSourceID;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static java.lang.String.format;

@ImplementedBy(NaptanDataCallbackImporter.class)
public abstract class NaptanDataImporter {
    private final RemoteDataAvailable remoteDataRefreshed;
    private final boolean enabled;

    private static final Logger logger = LoggerFactory.getLogger(NaptanDataImporter.class);

    protected NaptanDataImporter(RemoteDataAvailable remoteDataRefreshed, TramchesterConfig config, FetchDataFromUrl.Ready dataIsReady) {
        this.remoteDataRefreshed = remoteDataRefreshed;
        this.enabled = config.hasRemoteDataSourceConfig(DataSourceID.naptanxml);
    }

    public void loadData(final ElementsFromXMLFile.XmlElementConsumer<NaptanStopData> receiver) {
        if (!enabled) {
            logger.warn("Not enabled");
            return;
        }

        if (!remoteDataRefreshed.hasFileFor(DataSourceID.naptanxml)) {
            final String message = "Missing source file for " + DataSourceID.naptanxml;
            logger.error(message);
            throw new RuntimeException(message);
        }

        Path filePath = remoteDataRefreshed.fileFor(DataSourceID.naptanxml);
        final String name = filePath.getFileName().toString();

        if (name.toLowerCase().endsWith(".zip")) {
            String newPath = FilenameUtils.removeExtension(filePath.toString());
            logger.info(format("Zip was downloaded as %s, use unzipped file %s", filePath, newPath));
            filePath = Path.of(newPath);
        }

        logger.info("Loading data from " + filePath.toAbsolutePath());
        // naptan xml is UTF-8
        loadDataFromFile(filePath, receiver);

        receiver.logSkipped(logger);

    }

    abstract void loadDataFromFile(Path filePath, ElementsFromXMLFile.XmlElementConsumer<NaptanStopData> consumer);

    public boolean isEnabled() {
        return enabled;
    }

}

