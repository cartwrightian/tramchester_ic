package com.tramchester.graph.search.diagnostics;

import com.google.inject.Inject;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.core.TimedPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@LazySingleton
public class RecordJourneyGraphPath {
    private static final Logger logger = LoggerFactory.getLogger(RecordJourneyGraphPath.class);


    private boolean enabled;
    private final Map<UUID, List<TimedPath>> paths;

    @Inject
    RecordJourneyGraphPath() {
        enabled = false;
        paths = new HashMap<>();
    }

    public void record(final UUID uid, final TimedPath path) {
        if (enabled) {
            logger.debug("enabled, path for %s was %s".formatted(uid, path.toString()));
            if (!paths.containsKey(uid)) {
                paths.put(uid, new ArrayList<>());
            }
            paths.get(uid).add(path);
        }
    }

    public List<TimedPath> getPathsFor(UUID uid) {
        if (!enabled) {
            throw new RuntimeException("Not enabled, diag/test support, must be enabled first");
        }
        if (!paths.containsKey(uid)) {
            throw new RuntimeException("No paths for " + uid);
        }
        return paths.get(uid);
    }

    public void enable() {
        logger.warn("enabling");
        enabled = true;
    }

    public boolean hasPathsFor(UUID uid) {
        return paths.containsKey(uid);
    }
}
