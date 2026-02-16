package com.tramchester.graph.core.inMemory;

import com.netflix.governator.guice.lazy.LazySingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@LazySingleton
public class GraphIdFactory {
    private static final Logger logger = LoggerFactory.getLogger(GraphCore.class);

    private final AtomicInteger currentGraphNodeId;
    private final AtomicInteger currentRelationshipId;

    public GraphIdFactory() {
        currentGraphNodeId = new AtomicInteger(0);
        currentRelationshipId = new AtomicInteger(0);
    }

    @PostConstruct
    void start() {
        logger.info("Started " + this);
    }

    public static boolean same(GraphIdFactory idFactoryA, GraphIdFactory idFactoryB) {
        // to support comparison of loaded graphs
        return idFactoryA.equals(idFactoryB);
    }

    public int getNextNodeId() {
        return currentGraphNodeId.incrementAndGet();
    }

    public int getNextRelationshipId() {
        return currentRelationshipId.incrementAndGet();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GraphIdFactory that = (GraphIdFactory) o;
        return Objects.equals(currentGraphNodeId.get(), that.currentGraphNodeId.get()) &&
                Objects.equals(currentRelationshipId.get(), that.currentRelationshipId.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentGraphNodeId, currentRelationshipId);
    }

    @Override
    public String toString() {
        return "GraphIdFactory{" +
                "nextGraphNodeId=" + currentGraphNodeId.get() +
                ", nextRelationshipId=" + currentRelationshipId.get() +
                '}';
    }

    /***
     * support loading from files
     * @param id sets new current node id
     */
    public void captureNodeId(int id) {
        logger.warn("Setting Current Node Id to " + id);
        currentGraphNodeId.set(id);
    }

    /***
     * support loading from files
     * @param id sets new current node id
     */
    public void captureRelationshipId(int id) {
        logger.warn("Setting Current Relationship Id to " + id);
        currentRelationshipId.set(id);
    }
}
