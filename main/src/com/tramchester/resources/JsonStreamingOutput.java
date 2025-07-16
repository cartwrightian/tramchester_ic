package com.tramchester.resources;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.tramchester.graph.facade.MutableGraphTransactionNeo4J;
import jakarta.ws.rs.core.StreamingOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Stream;

public class JsonStreamingOutput<T> implements StreamingOutput {
    private static final Logger logger = LoggerFactory.getLogger(JsonStreamingOutput.class);

    private final Stream<T> theStream;
    private final MutableGraphTransactionNeo4J txn;

    private final JsonFactory jsonFactory ;

    JsonStreamingOutput(MutableGraphTransactionNeo4J txn, Stream<T> theStream) {
        this.txn = txn;
        this.theStream = theStream;
        JsonMapper mapper = JsonMapper.builder().
                addModule(new JavaTimeModule()).
                addModule(new AfterburnerModule()).
                build();
        jsonFactory = mapper.getFactory();
    }

    public JsonStreamingOutput(Stream<T> theStream) {
        this(null, theStream);
    }

    /**
     * Writes theStream to outputStream, closes theStream and the txn (if present)
     * @param outputStream the stream being written to
     */
    @Override
    public void write(final OutputStream outputStream)  {
        // NOTE: by default there is an 8K output buffer on outputStream

        logger.info("Write stream to response");

        theStream.onClose(() -> {
            logger.info("Closed source stream");
            if (txn!=null) {
                logger.info("Closing transaction");
                txn.close();
            }
        });


        try (final JsonGenerator jsonGenerator = jsonFactory.createGenerator(outputStream)) {
            jsonGenerator.writeStartArray();
            theStream.forEach(item -> {
                synchronized (outputStream) {
                    try {
                        jsonGenerator.writeObject(item);
                        jsonGenerator.writeString(System.lineSeparator());
                        jsonGenerator.writeString(System.lineSeparator());
                        jsonGenerator.flush();
                    } catch (IOException innerException) {
                        logger.error("Exception during streaming item " + item.toString(), innerException);
                    }
                }
            });
            jsonGenerator.writeEndArray();
            jsonGenerator.flush();
        }
        catch (IOException ioException) {
           logger.warn("Exception during streaming", ioException);
        } finally {
            theStream.close();
            logger.info("Stream closed");
        }
    }
}
