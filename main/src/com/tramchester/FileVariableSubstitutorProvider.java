package com.tramchester;

import com.tramchester.config.FileVariableSubstitutor;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class FileVariableSubstitutorProvider implements ConfigurationSourceProvider {

    // todo/note likely too early in lifecycle to wire up as expected
    private static final Logger logger = LoggerFactory.getLogger(FileVariableSubstitutorProvider.class);

    private final ConfigurationSourceProvider delegate;

    public FileVariableSubstitutorProvider(ConfigurationSourceProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public InputStream open(final String textPath) throws IOException {
        final Path path = Path.of(textPath);
        logger.debug("Source config is " + path.toAbsolutePath());

        final Path confirDir = path.getParent().toAbsolutePath();
        if (confirDir.toFile().isDirectory()) {
            logger.info("Will load included config files from " + confirDir);
        } else {
            String msg = confirDir + " is not a valid directory ";
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        final FileVariableSubstitutor substitutor = new FileVariableSubstitutor(confirDir);

        try (InputStream in = delegate.open(textPath)) {
            final String config = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            final String substituted = substitutor.replace(config);

            return new ByteArrayInputStream(substituted.getBytes(StandardCharsets.UTF_8));
        }
    }
}
