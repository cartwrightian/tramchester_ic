package com.tramchester.integration.testSupport.config;

import com.tramchester.config.*;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.TestGroupType;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Train;
import static java.lang.String.format;

public class GraphDBTestConfig implements GraphDBConfig {
    private final TestGroupType group;
    private final TramchesterConfig config;
    private final String pathPrefix;

    public GraphDBTestConfig(TestGroupType group, TramchesterConfig config) {
        this("", group, config);
    }

    public GraphDBTestConfig(final String pathPrefix, TestGroupType group, TramchesterConfig config) {
        this.pathPrefix = pathPrefix;
        this.group = group;
        this.config = config;
    }

    public Path getDbPath() {
        return createGraphDatabasePath(group);
    }

    @Override
    public String getNeo4jPagecacheMemory() {
        final EnumSet<TransportMode> transportModes = config.getTransportModes();
        if (transportModes.contains(Bus)) {
            return "300m";
        }
        if (transportModes.contains(Train)) {
            return "1000m";
        }
        return "100m";
    }

    @Override
    public String getMemoryTransactionGlobalMaxSize() {
        return "650m";
    }

    @Override
    public Boolean enableDiagnostics() {
        return false;
    }

    private Path createGraphDatabasePath(final TestGroupType group) {
        final Set<DataSourceID> sourcesFromConfig = config.getRemoteDataSourceConfig().stream().
                map(RemoteDataSourceConfig::getDataSourceId).
                filter(dataSourceId -> dataSourceId !=DataSourceID.database).
                collect(Collectors.toSet());

        final List<GTFSSourceConfig> gtfsDataSource = config.getGTFSDataSource();
        final Set<TransportMode> groupedModesFromConfig = gtfsDataSource.stream().
                flatMap(gtfsSourceConfig -> gtfsSourceConfig.groupedStationModes().stream()).
                collect(Collectors.toSet());

        final boolean hasClosures = gtfsDataSource.stream().anyMatch(gtfsSourceConfig -> !gtfsSourceConfig.getStationClosures().isEmpty());

        final EnumSet<TransportMode> modesFromConfig = config.getTransportModes();
        String modes = modesFromConfig.isEmpty() ? "NoModesEnabled" : asText(modesFromConfig);

        String subFolderName = pathPrefix.isEmpty() ? format("%s_%s", group, modes) : format("%s_%s_%s", pathPrefix, group, modes);

        if (!sourcesFromConfig.isEmpty()) {
            subFolderName = subFolderName + "_sources_" + asText(sourcesFromConfig);
        }

        String dbName = "tramchester_database";
        if (!groupedModesFromConfig.isEmpty()) {
            dbName = dbName + "_grouped_" + asText(groupedModesFromConfig);
        }
        if (config.hasNeighbourConfig()) {
            dbName = dbName + "_withNeighbours";
        }
        if (hasClosures) {
            dbName = dbName + closuresText(gtfsDataSource);
        }
        if (config.isGraphFiltered()) {
            // TODO proper naming
            dbName = dbName + "_isFiltered";
        }

        Path containingFolder = Path.of("databases", subFolderName);
        return containingFolder.resolve(dbName + ".db");
    }

    private static @NotNull String closuresText(final List<GTFSSourceConfig> gtfsDataSource) {
        // iff closures are in effect
        // then....
        final List<String> items = gtfsDataSource.stream().
                flatMap(gtfsConfig -> gtfsConfig.getStationClosures().stream()).
                map(GraphDBTestConfig::closuresText).
                sorted().
                toList();

        final StringBuilder result = new StringBuilder();
        for(final String text : items) {
            if (!result.isEmpty()) {
                result.append("_");
            }
            result.append(text);
        }
        return "_withClosures_"+result;
    }

    private static String closuresText(final StationClosures stationClosuresConfig) {

        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE;

        final DateRange dateRange = stationClosuresConfig.getDateRange();
        final LocalDate startDate = dateRange.getStartDate().toLocalDate();
        final LocalDate endDate = dateRange.getEndDate().toLocalDate();

        final StationsConfig stationClosures = stationClosuresConfig.getStations();

        final String ids = StationsConfig.getStationsFrom(stationClosures).stream().
                map(IdFor::getGraphId).
                sorted().
                reduce("", (a, b) -> a + b);

//        final String ids;
//
//        if (stationClosures instanceof StationListConfig stationListConfig) {
//            ids = stationListConfig.getStations().stream().
//                    map(IdFor::getGraphId).
//                    reduce("", (a, b) -> a + b);
//        } else if (stationClosures instanceof StationPairConfig stationPairConfig) {
//            ids = String.format("%s_to_%s", stationPairConfig.getFirst(), stationPairConfig.getSecond());
//        } else {
//            throw new RuntimeException("Unexpected StationClosures " + stationClosures);
//        }

        return String.format("%s_%s_%s", dateTimeFormatter.format(startDate), dateTimeFormatter.format(endDate), ids);
    }

    // need consistent ordering here to prevent duplication
    private <E extends Enum<E>> String asText(final Set<E> enums) {

        final List<E> ordered = enums.stream().sorted(Comparator.comparing(Enum::name)).toList();

        final StringBuilder builder = new StringBuilder();
        for (E value : ordered) {
            if (!builder.isEmpty()) {
                builder.append("_");
            }
            builder.append(value.name());
        }
        return builder.toString();
    }

}
