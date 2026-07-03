package com.tramchester.dataimport.NaPTAN.xml;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.NaPTAN.xml.stopPoint.NaptanStopData;
import com.tramchester.dataimport.RemoteDataAvailable;
import com.tramchester.dataimport.loader.files.ElementsFromXMLFile;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class NaptanDataCallbackImporter extends NaptanDataImporter {
    private static final Logger logger = LoggerFactory.getLogger(NaptanDataCallbackImporter.class);

    private final XmlMapper mapper;

    @Inject
    public NaptanDataCallbackImporter(RemoteDataAvailable remoteDataRefreshed, TramchesterConfig config,
                                      FetchDataFromUrl.Ready dataIsReady) {
        super(remoteDataRefreshed, config, dataIsReady);

        mapper = XmlMapper.builder().
                addModule(new BlackbirdModule()).
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).
                disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).
                enable(DeserializationFeature.EAGER_DESERIALIZER_FETCH).
                build();
    }

    @Override
    void loadDataFromFile(Path filePath, ElementsFromXMLFile.XmlElementConsumer<NaptanStopData> consumer) {

        final ElementsFromXMLFile<NaptanStopData> dataLoader = new ElementsFromXMLFile<>(filePath,
                StandardCharsets.UTF_8, mapper, consumer);

        logger.info("Loading data from " + filePath.toAbsolutePath());
        dataLoader.load();
    }

}
