package com.tramchester.dataimport.nptg;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.RemoteDataAvailable;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.dataimport.loader.files.ElementsFromXMLFile;
import com.tramchester.dataimport.nptg.xml.NPTGLocalityXMLData;
import com.tramchester.domain.DataSourceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@LazySingleton
public class NPTGXMLDataLoader {
    public static final String LOCALITIES_XML = "NPTG.xml";
    private static final Logger logger = LoggerFactory.getLogger(NPTGLocalityXMLData.class);

    private final RemoteDataAvailable remoteDataRefreshed;
    private final XmlMapper mapper;
    private final boolean enabled;

    @Inject
    public NPTGXMLDataLoader(TramchesterConfig config, UnzipFetchedData.Ready dataIsReady, RemoteDataAvailable remoteDataRefreshed) {
        enabled = config.hasRemoteDataSourceConfig(DataSourceID.nptg);
        this.remoteDataRefreshed = remoteDataRefreshed;
        mapper = XmlMapper.builder().
                addModule(new BlackbirdModule()).
//                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).
//                disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).
//                enable(DeserializationFeature.EAGER_DESERIALIZER_FETCH).
                build();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void loadData(ElementsFromXMLFile.XmlElementConsumer<NPTGLocalityXMLData> consumer) {
        if (!enabled) {
            logger.warn("Not enabled");
            return;
        }

        if (!remoteDataRefreshed.hasFileFor(DataSourceID.nptg)) {
            final String message = "Missing source file for " + DataSourceID.nptg;
            logger.error(message);
            throw new RuntimeException(message);
        }

        Path filePath = remoteDataRefreshed.fileFor(DataSourceID.nptg);

        logger.info("Loading data from " + filePath.toAbsolutePath());

        ElementsFromXMLFile<NPTGLocalityXMLData> dataLoader = new ElementsFromXMLFile<>(filePath, StandardCharsets.UTF_8, mapper, consumer);

        dataLoader.load();
    }
}
