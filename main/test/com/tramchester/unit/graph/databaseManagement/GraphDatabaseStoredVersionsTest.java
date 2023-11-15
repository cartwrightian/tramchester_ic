package com.tramchester.unit.graph.databaseManagement;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.facade.MutableGraphTransaction;
import com.tramchester.graph.facade.GraphTransactionFactory;
import com.tramchester.graph.databaseManagement.GraphDatabaseMetaInfo;
import com.tramchester.graph.databaseManagement.GraphDatabaseStoredVersions;
import com.tramchester.repository.DataSourceRepository;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
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
    private MutableGraphTransaction transaction;
    private TramchesterConfig config;
    private DataSourceRepository dataSourceRepository;
    private Duration timeout = GraphDatabase.DEFAULT_TXN_TIMEOUT;

    @BeforeEach
    public void beforeAnyTestsRun() {

        config = TestEnv.GET();
        databaseMetaInfo = createMock(GraphDatabaseMetaInfo.class);
        transactionFactory = createMock(GraphTransactionFactory.class);
        transaction = createMock(MutableGraphTransaction.class);
        storedVersions = new GraphDatabaseStoredVersions(config, databaseMetaInfo);
        dataSourceRepository = createMock(DataSourceRepository.class);
    }

    @Test
    public void shouldOutOfDateIfNeighboursNot() {

        EasyMock.expect(transactionFactory.begin(timeout)).andReturn(transaction);
        EasyMock.expect(databaseMetaInfo.isNeighboursEnabled(transaction)).andReturn(!config.hasNeighbourConfig());
        transaction.close();
        EasyMock.expectLastCall();

        replayAll();
        boolean result = storedVersions.upToDate(transactionFactory, dataSourceRepository);
        verifyAll();

        assertFalse(result);
    }

    @Test
    public void shouldOutOfDateIfVersionMissingFromDB() {

        EasyMock.expect(transactionFactory.begin(timeout)).andReturn(transaction);
        EasyMock.expect(databaseMetaInfo.isNeighboursEnabled(transaction)).andReturn(config.hasNeighbourConfig());
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

        dataSourceInfo.add(new DataSourceInfo(tfgm, "v1.1", LocalDateTime.MIN, EnumSet.of(Tram)));
        dataSourceInfo.add(new DataSourceInfo(naptanxml, "v2.3", LocalDateTime.MIN, EnumSet.of(Bus)));

        EasyMock.expect(dataSourceRepository.getDataSourceInfo()).andReturn(dataSourceInfo);

        versionMap.put("tfgm", "v1.1");
        versionMap.put("naptanxml", "v2.3");

        EasyMock.expect(transactionFactory.begin(timeout)).andReturn(transaction);
        EasyMock.expect(databaseMetaInfo.isNeighboursEnabled(transaction)).andReturn(config.hasNeighbourConfig());
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

        dataSourceInfo.add(new DataSourceInfo(tfgm, "v1.2", LocalDateTime.MIN, EnumSet.of(Tram)));
        dataSourceInfo.add(new DataSourceInfo(naptanxml, "v2.3", LocalDateTime.MIN, EnumSet.of(Bus)));
        EasyMock.expect(dataSourceRepository.getDataSourceInfo()).andReturn(dataSourceInfo);

        versionMap.put("tfgm", "v1.1");
        versionMap.put("naptanxml", "v2.3");

        EasyMock.expect(transactionFactory.begin(timeout)).andReturn(transaction);
        EasyMock.expect(databaseMetaInfo.isNeighboursEnabled(transaction)).andReturn(config.hasNeighbourConfig());
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

        dataSourceInfo.add(new DataSourceInfo(tfgm, "v1.2", LocalDateTime.MIN, EnumSet.of(Tram)));
        dataSourceInfo.add(new DataSourceInfo(naptanxml, "v2.3", LocalDateTime.MIN, EnumSet.of(Bus)));
        EasyMock.expect(dataSourceRepository.getDataSourceInfo()).andReturn(dataSourceInfo);

        versionMap.put("tfgm", "v1.1");

        EasyMock.expect(transactionFactory.begin(timeout)).andReturn(transaction);
        EasyMock.expect(databaseMetaInfo.isNeighboursEnabled(transaction)).andReturn(config.hasNeighbourConfig());
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

        dataSourceInfo.add(new DataSourceInfo(tfgm, "v1.2", LocalDateTime.MIN, EnumSet.of(Tram)));
        EasyMock.expect(dataSourceRepository.getDataSourceInfo()).andReturn(dataSourceInfo);

        versionMap.put("tfgm", "v1.1");
        versionMap.put("naptanxml", "v2.3");

        EasyMock.expect(transactionFactory.begin(timeout)).andReturn(transaction);
        EasyMock.expect(databaseMetaInfo.isNeighboursEnabled(transaction)).andReturn(config.hasNeighbourConfig());
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
