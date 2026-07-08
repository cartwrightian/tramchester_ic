package com.tramchester.dataimport.loader.files;

import com.ctc.wstx.stax.WstxInputFactory;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.function.Consumer;

public class ElementsFromXMLFile<T> {
    private static final Logger logger = LoggerFactory.getLogger(ElementsFromXMLFile.class);

    private final Path filePath;
    private final Charset charset;
    private final XmlMapper mapper;
    private final XmlElementConsumer<T> xmlElementConsumer;
    private final String requiredElementName;
    private final WstxInputFactory wstxInputFactory;

    private final JavaType elementJavaType;

    public ElementsFromXMLFile(Path filePath, Charset charset, XmlMapper mapper, XmlElementConsumer<T> xmlElementConsumer) {
        this.filePath = filePath.toAbsolutePath();
        this.charset = charset;
        this.mapper = mapper;
        this.xmlElementConsumer = xmlElementConsumer;

        wstxInputFactory = new WstxInputFactory();
        // in practice this seems to make little difference....
        wstxInputFactory.configureForSpeed();

        final Class<T> elementType = xmlElementConsumer.getElementType();
        requiredElementName = getElementName(elementType);
        elementJavaType = mapper.getTypeFactory().constructType(elementType);
    }

    private String getElementName(final Class<?> type) {
        final JsonTypeName elementType = type.getAnnotation(JsonTypeName.class);
        return elementType.value();
    }

    public void load() {
        logger.info("Load xml data from " +filePath.toAbsolutePath() + " for " + elementJavaType);
        try {
            final FileInputStream fileInputStream = new FileInputStream(filePath.toFile());
            final BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            load(bufferedInputStream);
            bufferedInputStream.close();
        } catch (IOException | XMLStreamException e) {
            String msg = "Unable to load from file " + filePath;
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public void load(final InputStream inputStream) throws XMLStreamException, IOException {

        final XMLStreamReader xmlReader = wstxInputFactory.createXMLStreamReader(inputStream, charset.name());
        final XmlFactory xmlFactory = mapper.getFactory();
        final ObjectReader reader = mapper.readerFor(elementJavaType);

        logger.info("Begin load");

        final XmlFilteredStreamReader streamReader = new XmlFilteredStreamReader(xmlReader, xmlFactory, requiredElementName);

        while (streamReader.matches()) {
            consumeRequiredElement(reader, streamReader.getParser());
        }

        streamReader.close();
        logger.info("Finished load");

    }

    private void consumeRequiredElement(final ObjectReader reader, final FromXmlParser parser) throws IOException {
//        final TreeNode tree = reader.readTree(parser);
//        final T element = reader.readValue(tree.traverse(mapper));
        final T element = reader.readValue(parser);
        xmlElementConsumer.process(element);
    }

    public abstract static class XmlElementConsumer<T> {
        private final Class<T> elementType;
        private final Consumer<T> consumer;
        private int skippedStop;

        protected XmlElementConsumer(final Class<T> elementType, final Consumer<T> consumer) {
            this.elementType = elementType;
            this.consumer = consumer;
            skippedStop = 0;
        }

        protected void process(final T element) {
            if (shouldInclude(element)) {
                consumer.accept(element);
            } else {
                skippedStop++;
            }
        }

        protected abstract boolean shouldInclude(final T item);

        Class<T> getElementType() {
            return elementType;
        }

        public void logSkipped(Logger logger) {
            if (skippedStop>0) {
                logger.info("Skipped " + skippedStop + " items of type " + elementType.getSimpleName());
            }
        }
    }

}
