package com.tramchester.unit.dataExport;

import com.tramchester.dataexport.Zipper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ZipperTest {

    public static final String CONFIG_DIR = "config";
    private final Path testFilePath = Path.of("testFile.txt");
    private Zipper zipper;

    @BeforeEach
    void beforeEachTestRuns() {
        zipper = new Zipper();
    }

    @AfterEach
    void afterEachTestRuns() throws IOException {
        Files.deleteIfExists(testFilePath);
    }

    @Test
    void shouldZipThings() throws IOException {

        ByteArrayOutputStream zippedStream = zipper.zip(Path.of(CONFIG_DIR).toAbsolutePath());

        ByteArrayInputStream inputStream = new ByteArrayInputStream(zippedStream.toByteArray());

        ZipInputStream result = new ZipInputStream(inputStream);

        List<ZipEntry> all = new ArrayList<>();

        ZipEntry next = result.getNextEntry();
        while (next!=null) {
            all.add(next);
            next = result.getNextEntry();
        }

        List<ZipEntry> dir = all.stream().filter(ZipEntry::isDirectory).toList();

        assertEquals(1, dir.size());
        assertEquals(CONFIG_DIR + File.separator, dir.get(0).getName());

        File[] onDisk = new File(CONFIG_DIR).listFiles();
        assertNotNull(onDisk);

        List<ZipEntry> files = all.stream().filter(zipEntry -> !zipEntry.isDirectory()).toList();
        assertEquals(onDisk.length, files.size(), files.toString());

        all.forEach(fromZip -> {
            String name = fromZip.getName();
            File file = new File(name);
            assertTrue(file.exists(), name);
            if (!file.isDirectory()) {
                assertEquals(file.length(), fromZip.getSize(), name);
            }

            long diskLastMod = Instant.ofEpochMilli(file.lastModified()).getEpochSecond();
            long zipLastMod = Instant.ofEpochMilli(fromZip.getLastModifiedTime().toMillis()).getEpochSecond();

            assertEquals(diskLastMod, zipLastMod, name);
        });
    }

    @Test
    void shouldZipSingleFileAsExpected() throws IOException {
        final String text = "someTextInAFileToUploadToS3";
        Files.writeString(testFilePath, text);

        ByteArrayOutputStream zippedStream = zipper.zip(testFilePath);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(zippedStream.toByteArray());

        ZipInputStream result = new ZipInputStream(inputStream);

        ZipEntry entry = result.getNextEntry();
        assertNotNull(entry);

        assertEquals(testFilePath.getFileName().toString(), entry.getName());

        String zipText = new String(result.readAllBytes());
        assertEquals(text, zipText);


    }

    @Disabled("slow")
    @Test
    void shouldSanityCheckZip() throws IOException {
        Zipper zipper = new Zipper();
        zipper.zip(Path.of("databases", "tramchester.db"));
    }

}
