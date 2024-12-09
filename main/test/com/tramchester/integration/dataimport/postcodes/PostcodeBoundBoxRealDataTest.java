package com.tramchester.integration.dataimport.postcodes;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.dataimport.loader.files.TransportDataFromCSVFile;
import com.tramchester.dataimport.postcodes.PostcodeBoundingBoxs;
import com.tramchester.dataimport.postcodes.PostcodeData;
import com.tramchester.domain.DataSourceID;
import com.tramchester.geo.*;
import com.tramchester.integration.testSupport.tram.TramWithPostcodesEnabled;
import com.tramchester.repository.postcodes.PostcodeRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.PostcodeTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.KnownLocations.nearAltrincham;
import static com.tramchester.testSupport.reference.KnownLocations.nearShudehill;
import static org.junit.jupiter.api.Assertions.*;

public class PostcodeBoundBoxRealDataTest {

    private static final CsvMapper mapper = CsvMapper.builder().addModule(new AfterburnerModule()).build();

    private static ComponentContainer componentContainer;
    private static TramWithPostcodesEnabled config;
    private PostcodeBoundingBoxs boundingBoxs;
    private Path centralManchesterPostcodes;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new TramWithPostcodesEnabled();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        boundingBoxs = componentContainer.get(PostcodeBoundingBoxs.class);
        RemoteDataSourceConfig sourceConfig = config.getDataRemoteSourceConfig(DataSourceID.postcode);

        // force creation of hints file
        PostcodeRepository repository = componentContainer.get(PostcodeRepository.class);
        repository.start(); // creates bounds data
        boundingBoxs.stop(); // saves the hints file
        boundingBoxs.start(); // loads the file

        final Path file = Path.of("Data", "CSV", "m.csv");
        centralManchesterPostcodes = sourceConfig.getDataPath().resolve(file);
    }

    @Test
    void shouldHaveHintsFile() {
        Path hintsFile = config.getCacheFolder().resolve("postcode_hints.csv");
        assertTrue(hintsFile.toFile().exists());
    }

    @PostcodeTest
    @Test
    void shouldCalcCorrectBoundsFromData() {

        final BoundingBox boundsFor = boundingBoxs.getBoundsFor(centralManchesterPostcodes);
        assertNotNull(boundsFor);

        Set<PostcodeData> centralManchester = getPostcodeData(centralManchesterPostcodes);

        int minEasting = getMinimumFor(centralManchester, GridPosition::getEastings);
        assertEquals(minEasting, boundsFor.getMinEastings());

        int minNorthing = getMinimumFor(centralManchester, GridPosition::getNorthings);
        assertEquals(minNorthing, boundsFor.getMinNorthings());

        int maxEasting = getMaximumFor(centralManchester, GridPosition::getEastings);
        assertEquals(maxEasting, boundsFor.getMaxEasting());

        int maxNorthing = getMaximumFor(centralManchester, GridPosition::getNorthings);
        assertEquals(maxNorthing, boundsFor.getMaxNorthings());
    }

    @PostcodeTest
    @Test
    void shouldHaveExpectedOverlaps() {
        Set<PostcodeData> centralManchester = getPostcodeData(centralManchesterPostcodes);
        final BoundingBox boundsFor = boundingBoxs.getBoundsFor(centralManchesterPostcodes);
        assertNotNull(boundsFor);

        Set<GridPosition> validGrids = centralManchester.stream().
                map(PostcodeData::getGridPosition).
                filter(GridPosition::isValid).collect(Collectors.toSet());

        long matched = validGrids.stream().
                filter(grid -> boundsFor.within( MarginInMeters.ofMeters(0), grid)).count();

        assertEquals(validGrids.size(), matched);
    }

    @PostcodeTest
    @Test
    void shouldMatchStationLocationBounds() {
        StationLocations stationLocations = componentContainer.get(StationLocations.class);
        BoundingBox containsStations = stationLocations.getActiveStationBounds();
        final BoundingBox boundsForPostcodeFile = boundingBoxs.getBoundsFor(centralManchesterPostcodes);

        assertTrue(boundsForPostcodeFile.overlapsWith(containsStations),
                boundsForPostcodeFile + " no overlap with " + containsStations);
        assertTrue(containsStations.overlapsWith(boundsForPostcodeFile),
                containsStations + " no overlap with " + boundsForPostcodeFile);

    }

    @PostcodeTest
    @Test
    void shouldGetCodeForLocation() {
        // NOTE: these are bounding boxs which cover significantly more area than the postcodes themselves,
        // and can overlap
        Set<String> codes = boundingBoxs.getCodesFor(nearShudehill.grid(),
                MarginInMeters.ofMeters(0));
        assertTrue(codes.contains("m"));

        Set<String> codesForAlty = boundingBoxs.getCodesFor(nearAltrincham.grid(),
                MarginInMeters.ofMeters(0));
        assertTrue(codesForAlty.contains("wa"));

    }

    private int getMinimumFor(Set<PostcodeData> postcodes, Function<GridPosition, Integer> getGrid) {
        Optional<Integer> min = getNoneZeroFor(postcodes, getGrid).min(Long::compare);
        assertTrue(min.isPresent());
        return min.get();
    }

    private int getMaximumFor(Set<PostcodeData> postcodes, Function<GridPosition, Integer> getGrid) {
        Optional<Integer> max = getNoneZeroFor(postcodes, getGrid).max(Long::compare);
        assertTrue(max.isPresent());
        return max.get();
    }

    @NotNull
    private Stream<Integer> getNoneZeroFor(Set<PostcodeData> postcodes, Function<GridPosition, Integer> getGrid) {
        return postcodes.stream().
                map(PostcodeData::getGridPosition).
                map(getGrid).
                filter(value -> value > 0);
    }

    private Set<PostcodeData> getPostcodeData(Path file) {
        TransportDataFromCSVFile<PostcodeData, PostcodeData> loader = new TransportDataFromCSVFile<>(file, PostcodeData.class,
                PostcodeData.CVS_HEADER, mapper);
        return loader.load().collect(Collectors.toSet());
    }

}
