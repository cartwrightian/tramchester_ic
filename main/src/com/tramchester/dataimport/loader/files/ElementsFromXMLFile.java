package com.tramchester.dataimport.loader.files;

import com.ctc.wstx.stax.WstxInputFactory;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;

public class ElementsFromXMLFile<T> {
    private static final Logger logger = LoggerFactory.getLogger(ElementsFromXMLFile.class);

    private final Path filePath;
    private final Charset charset;
    private final XmlMapper mapper;
    private final XmlElementConsumer<T> xmlElementConsumer;
    private final WstxInputFactory factory;
    private final String stopElementName;

    private final JavaType elementJavaType;

    public ElementsFromXMLFile(Path filePath, Charset charset, XmlMapper mapper, XmlElementConsumer<T> xmlElementConsumer) {
        this.filePath = filePath.toAbsolutePath();
        this.charset = charset;
        this.mapper = mapper;
        this.xmlElementConsumer = xmlElementConsumer;

        factory = new WstxInputFactory();

        final Class<T> elementType = xmlElementConsumer.getElementType();
        stopElementName = getElementName(elementType);
        elementJavaType = mapper.getTypeFactory().constructType(elementType);

    }

    private String getElementName(final Class<?> type) {
        final JsonTypeName elementType = type.getAnnotation(JsonTypeName.class);
        return elementType.value();
    }

    public void load() {
        try {
            final Reader fileReader = new FileReader(filePath.toString(), charset);
            final BufferedReader reader = new BufferedReader(fileReader);
            logger.info("Load xml data from " +filePath.toAbsolutePath() + " for " + elementJavaType);
            load(reader);
            reader.close();
        } catch (IOException | XMLStreamException e) {
            String msg = "Unable to load from file " + filePath;
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public void load(final Reader in) throws XMLStreamException, IOException {

        logger.info("Begin load");
        final XMLStreamReader streamReader = factory.createXMLStreamReader(in);

        while (streamReader.hasNext()) {
            if (streamReader.isStartElement()) {
                final String localName = streamReader.getLocalName();
                if (stopElementName.equals(localName)) {
                    consumeStopElement(streamReader);
                } else {
                    streamReader.next();
                }
            } else {
                streamReader.next();
            }
        }

        streamReader.close();
        logger.info("Finished load");

    }

    private void consumeStopElement(final XMLStreamReader in) throws IOException {
        final T element = mapper.readValue(in, elementJavaType);
        xmlElementConsumer.process(element);
    }

    public interface XmlElementConsumer<T> {
        void process(final T element);

        Class<T> getElementType();
    }

}
