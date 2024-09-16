package com.tramchester.dataimport;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.ZonedDateTime;

public interface DownloadAndModTime {
    URLStatus getStatusFor(URI uri, ZonedDateTime localModTime, boolean warnIfMissing) throws IOException, InterruptedException;

    URLStatus downloadTo(Path path, URI uri, ZonedDateTime localModTime) throws IOException;

}
