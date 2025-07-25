package com.tramchester.unit.graph.databaseManagement;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.URLStatus;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.geo.BoundingBox;
import com.tramchester.graph.GraphDatabaseNeo4J;
import com.tramchester.graph.databaseManagement.GraphDatabaseMetaInfo;
import com.tramchester.graph.databaseManagement.GraphDatabaseStoredVersions;
import com.tramchester.graph.facade.neo4j.GraphTransactionFactory;
import com.tramchester.graph.facade.neo4j.MutableGraphTransactionNeo4J;
import com.tramchester.repository.DataSourceRepository;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;

import static com.tramchester.domain.DataSourceID.naptanxml;
import static com.tramchester.domain.DataSourceID.tfgm;
import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphDatabaseStoredVersionsTest extends EasyMockSupport {

    private GraphDatabaseMetaInfo databaseMetaInfo;
    private GraphDatabaseStoredVersions storedVersions;
    private GraphTransactionFactory transactionFactory;
    private MutableGraphTransactionNeo4J transaction;
    private TramchesterConfig config;
    private DataSourceRepository dataSourceRepository;
    private final Duration timeout = GraphDatabaseNeo4J.DEFAULT_TXN_TIMEOUT;

    @BeforeEach
    public void beforeAnyTestsRun() {

        config = TestEnv.GET();
        databaseMetaInfo = createMock(GraphDatabaseMetaInfo.class);
        transactionFactory = createMock(GraphTransactionFactory.class);
        transaction = createMock(MutableGraphTransactionNeo4J.class);
        storedVersions = new GraphDatabaseStoredVersions(config, databaseMetaInfo);
        dataSourceRepository = createMock(DataSourceRepository.class);
    }

    @Test
    public void shouldOutOfDateIfNeighboursNot() {

        EasyMock.expect(transactionFactory.beginMutable(timeout)).andReturn(transaction);
        EasyMock.expect(databaseMetaInfo.isNeighboursEnabled(transaction)).andReturn(!config.hasNeighbourConfig());
        transaction.close();
        EasyMock.expectLastCall();

        replayAll();
        boolean result = storedVersions.upToDate(transactionFactory, dataSourceRepository);
        verifyAll();

        assertFalse(result);
    }

    @Test
    public void shouldOutOfDateIfBoundsMismatch() {

        BoundingBox boundingBox = config.getBounds();

        EasyMock.expect(transactionFactory.beginMutable(timeout)).andReturn(transaction);
        EasyMock.expect(databaseMetaInfo.isNeighboursEnabled(transaction)).andReturn(config.hasNeighbourConfig());
        EasyMock.expect(databaseMetaInfo.boundsMatch(transaction, boundingBox)).andReturn(false);
        transaction.close();
        EasyMock.expectLastCall();

        replayAll();
        boolean result = storedVersions.upToDate(transactionFactory, dataSourceRepository);
        verifyAll();

        assertFalse(result);
    }

    @Test
    public void shouldOutOfDateIfVersionMissingFromDB() {

        EasyMock.expect(transactionFactory.beginMutable(timeout)).andReturn(transaction);
        EasyMock.expect(databaseMetaInfo.isNeighboursEnabled(transaction)).andReturn(config.hasNeighbourConfig());
        EasyMock.expect(databaseMetaInfo.boundsMatch(transaction, config.getBounds())).andReturn(true);
        EasyMock.expect(databaseMetaInfo.hasVersionInfo(transaction)).andReturn(false);
        transaction.close();
        EasyMock.expectLastCall();

        replayAll();
        boolean result = storedVersions.upToDate(transactionFactory, dataSourceRepository);
        verifyAll();

        assertFalse(result);
    }

    @Test
    public void shouldBeUpToDateIfVersionsFromDBMatch() {
        Set<DataSourceInfo> dataSourceInfo = new HashSet<>();
        Map<String, String> versionMap = new HashMap<>();

        dataSourceInfo.add(new DataSourceInfo(tfgm, "v1.1", URLStatus.invalidTime, EnumSet.of(Tram)));
        dataSourceInfo.add(new DataSourceInfo(naptanxml, "v2.3", URLStatus.invalidTime, EnumSet.of(Bus)));

        EasyMock.expect(dataSourceRepository.getDataSourceInfo()).andReturn(dataSourceInfo);

        versionMap.put("tfgm", "v1.1");
        versionMap.put("naptanxml", "v2.3");

        EasyMock.expect(transactionFactory.beginMutable(timeout)).andReturn(transaction);
        EasyMock.expect(databaseMetaInfo.isNeighboursEnabled(transaction)).andReturn(config.hasNeighbourConfig());
        EasyMock.expect(databaseMetaInfo.boundsMatch(transaction, config.getBounds())).andReturn(true);

        EasyMock.expect(databaseMetaInfo.hasVersionInfo(transaction)).andReturn(true);
        EasyMock.expect(databaseMetaInfo.getVersions(transaction)).andReturn(versionMap);
        transaction.close();
        EasyMock.expectLastCall();

        replayAll();
        boolean result = storedVersions.upToDate(transactionFactory, dataSourceRepository);
        verifyAll();

        assertTrue(result);
    }

    @Test
    public void shouldOutOfDateIfVersionsNumbersFromDBMisMatch() {
        Set<DataSourceInfo> dataSourceInfo = new HashSet<>();
        Map<String, String> versionMap = new HashMap<>();

        dataSourceInfo.add(new DataSourceInfo(tfgm, "v1.2", URLStatus.invalidTime, EnumSet.of(Tram)));
        dataSourceInfo.add(new DataSourceInfo(naptanxml, "v2.3", URLStatus.invalidTime, EnumSet.of(Bus)));
        EasyMock.expect(dataSourceRepository.getDataSourceInfo()).andReturn(dataSourceInfo);

        versionMap.put("tfgm", "v1.1");
        versionMap.put("naptanxml", "v2.3");

        EasyMock.expect(transactionFactory.beginMutable(timeout)).andReturn(transaction);
        EasyMock.expect(databaseMetaInfo.isNeighboursEnabled(transaction)).andReturn(config.hasNeighbourConfig());
        EasyMock.expect(databaseMetaInfo.boundsMatch(transaction, config.getBounds())).andReturn(true);
        EasyMock.expect(databaseMetaInfo.hasVersionInfo(transaction)).andReturn(true);
        EasyMock.expect(databaseMetaInfo.getVersions(transaction)).andReturn(versionMap);
        transaction.close();
        EasyMock.expectLastCall();

        replayAll();
        boolean result = storedVersions.upToDate(transactionFactory, dataSourceRepository);
        verifyAll();

        assertFalse(result);
    }

    @Test
    public void shouldOutOfDateIfVersionsFromDBMisMatch() {
        Set<DataSourceInfo> dataSourceInfo = new HashSet<>();
        Map<String, String> versionMap = new HashMap<>();

        dataSourceInfo.add(new DataSourceInfo(tfgm, "v1.2", URLStatus.invalidTime, EnumSet.of(Tram)));
        dataSourceInfo.add(new DataSourceInfo(naptanxml, "v2.3", URLStatus.invalidTime, EnumSet.of(Bus)));
        EasyMock.expect(dataSourceRepository.getDataSourceInfo()).andReturn(dataSourceInfo);

        versionMap.put("tfgm", "v1.1");

        EasyMock.expect(transactionFactory.beginMutable(timeout)).andReturn(transaction);
        EasyMock.expect(databaseMetaInfo.isNeighboursEnabled(transaction)).andReturn(config.hasNeighbourConfig());
        EasyMock.expect(databaseMetaInfo.boundsMatch(transaction, config.getBounds())).andReturn(true);
        EasyMock.expect(databaseMetaInfo.hasVersionInfo(transaction)).andReturn(true);
        EasyMock.expect(databaseMetaInfo.getVersions(transaction)).andReturn(versionMap);
        transaction.close();
        EasyMock.expectLastCall();

        replayAll();
        boolean result = storedVersions.upToDate(transactionFactory, dataSourceRepository);
        verifyAll();

        assertFalse(result);
    }

    @Test
    public void shouldOutOfDateIfVersionsFromDBMisMatchUnexpected() {
        Set<DataSourceInfo> dataSourceInfo = new HashSet<>();
        Map<String, String> versionMap = new HashMap<>();

        dataSourceInfo.add(new DataSourceInfo(tfgm, "v1.2", URLStatus.invalidTime, EnumSet.of(Tram)));
        EasyMock.expect(dataSourceRepository.getDataSourceInfo()).andReturn(dataSourceInfo);

        versionMap.put("tfgm", "v1.1");
        versionMap.put("naptanxml", "v2.3");

        EasyMock.expect(transactionFactory.beginMutable(timeout)).andReturn(transaction);
        EasyMock.expect(databaseMetaInfo.isNeighboursEnabled(transaction)).andReturn(config.hasNeighbourConfig());
        EasyMock.expect(databaseMetaInfo.boundsMatch(transaction, config.getBounds())).andReturn(true);
        EasyMock.expect(databaseMetaInfo.hasVersionInfo(transaction)).andReturn(true);
        EasyMock.expect(databaseMetaInfo.getVersions(transaction)).andReturn(versionMap);
        transaction.close();
        EasyMock.expectLastCall();

        replayAll();
        boolean result = storedVersions.upToDate(transactionFactory, dataSourceRepository);
        verifyAll();

        assertFalse(result);
    }

}
