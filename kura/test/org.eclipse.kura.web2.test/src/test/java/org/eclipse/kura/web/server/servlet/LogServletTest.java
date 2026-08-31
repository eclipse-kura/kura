/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LogServletTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final LogServlet logServlet = new LogServlet();

    /*
     * uniqueEntryName
     */

    @Test
    public void shouldKeepTheFileNameWhenThereIsNoCollision() {
        Set<String> usedEntryNames = new HashSet<>();

        assertEquals("kura.log", this.logServlet.uniqueEntryName(usedEntryNames, "kura.log"));
        assertEquals("messages", this.logServlet.uniqueEntryName(usedEntryNames, "messages"));
    }

    @Test
    public void shouldSuffixCollidingEntryNamesPreservingTheExtension() {
        Set<String> usedEntryNames = new HashSet<>();

        assertEquals("kura.log", this.logServlet.uniqueEntryName(usedEntryNames, "kura.log"));
        assertEquals("kura_1.log", this.logServlet.uniqueEntryName(usedEntryNames, "kura.log"));
        assertEquals("kura_2.log", this.logServlet.uniqueEntryName(usedEntryNames, "kura.log"));
    }

    @Test
    public void shouldSuffixCollidingEntryNamesWithoutExtension() {
        Set<String> usedEntryNames = new HashSet<>();

        assertEquals("messages", this.logServlet.uniqueEntryName(usedEntryNames, "messages"));
        assertEquals("messages_1", this.logServlet.uniqueEntryName(usedEntryNames, "messages"));
    }

    @Test
    public void shouldNotMistakeALeadingDotForAnExtension() {
        Set<String> usedEntryNames = new HashSet<>();

        assertEquals(".hidden", this.logServlet.uniqueEntryName(usedEntryNames, ".hidden"));
        assertEquals(".hidden_1", this.logServlet.uniqueEntryName(usedEntryNames, ".hidden"));
    }

    /*
     * buildArchiveName / sanitizeForFileName
     */

    @Test
    public void shouldNameTheArchiveAfterTheDeviceAndTheMoment() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 8, 18, 15, 26, 7);

        assertEquals("Kura_Logs_gateway-01_20260818-152607",
                this.logServlet.buildArchiveName("gateway-01", timestamp).replace(".zip", ""));
    }

    @Test
    public void shouldOmitTheDeviceNameWhenItIsMissing() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 8, 18, 15, 26, 7);

        assertEquals("Kura_Logs_20260818-152607.zip", this.logServlet.buildArchiveName(null, timestamp));
        assertEquals("Kura_Logs_20260818-152607.zip", this.logServlet.buildArchiveName("   ", timestamp));
        assertEquals("Kura_Logs_20260818-152607.zip", this.logServlet.buildArchiveName("///", timestamp));
    }

    @Test
    public void shouldKeepTheArchiveNameSafeForAFileSystemAndForTheHeader() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 8, 18, 15, 26, 7);

        assertEquals("Kura_Logs_my_device_20260818-152607.zip",
                this.logServlet.buildArchiveName("my/device", timestamp));
        assertEquals("Kura_Logs_dev_rm_-rf_20260818-152607.zip",
                this.logServlet.buildArchiveName("dev\"; rm -rf /", timestamp));
        assertEquals("Kura_Logs_ESF_GW_20260818-152607.zip",
                this.logServlet.buildArchiveName("ESF GW", timestamp));
    }

    @Test
    public void shouldCapAVeryLongDeviceName() {
        String longName = this.logServlet.sanitizeForFileName(
                "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghij");

        assertEquals(40, longName.length());
        assertEquals("abcdefghijabcdefghijabcdefghijabcdefghij", longName);
    }

    /*
     * zipFiles
     */

    @Test
    public void shouldZipAllProvidedFiles() throws IOException {
        List<File> files = new ArrayList<>();
        files.add(givenFile(this.folder.getRoot(), "kura.log", "kura content"));
        files.add(givenFile(this.folder.getRoot(), "messages", "system content"));

        Map<String, String> entries = whenFilesAreZipped(files);

        assertEquals(2, entries.size());
        assertEquals("kura content", entries.get("kura.log"));
        assertEquals("system content", entries.get("messages"));
    }

    @Test
    public void shouldSkipUnreadableFilesAndStillProduceAValidArchive() throws IOException {
        File missing = new File(this.folder.getRoot(), "rotated.log");

        List<File> files = new ArrayList<>();
        files.add(givenFile(this.folder.getRoot(), "kura.log", "kura content"));
        files.add(missing);
        files.add(givenFile(this.folder.getRoot(), "messages", "system content"));

        Map<String, String> entries = whenFilesAreZipped(files);

        assertFalse(missing.exists());
        assertEquals(2, entries.size());
        assertEquals("kura content", entries.get("kura.log"));
        assertEquals("system content", entries.get("messages"));
    }

    @Test
    public void shouldDeduplicateEntryNamesComingFromDifferentDirectories() throws IOException {
        File firstDir = this.folder.newFolder("var-log");
        File secondDir = this.folder.newFolder("opt-log");

        List<File> files = new ArrayList<>();
        files.add(givenFile(firstDir, "kura.log", "first content"));
        files.add(givenFile(secondDir, "kura.log", "second content"));

        Map<String, String> entries = whenFilesAreZipped(files);

        assertEquals(2, entries.size());
        assertEquals("first content", entries.get("kura.log"));
        assertEquals("second content", entries.get("kura_1.log"));
    }

    @Test
    public void shouldProduceAValidArchiveWhenThereIsNothingToZip() throws IOException {
        Map<String, String> entries = whenFilesAreZipped(new ArrayList<File>());

        assertTrue(entries.isEmpty());
    }

    /*
     * createPrivateTempDirectory / deleteTempDirectory
     */

    @Test
    public void shouldCreateDistinctPrivateTemporaryDirectories() throws IOException {
        Assume.assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));

        Path first = this.logServlet.createPrivateTempDirectory();
        Path second = this.logServlet.createPrivateTempDirectory();

        try {
            assertFalse(first.equals(second));
            assertTrue(Files.isDirectory(first));
            assertEquals(PosixFilePermissions.fromString("rwx------"), Files.getPosixFilePermissions(first));
        } finally {
            this.logServlet.deleteTempDirectory(first);
            this.logServlet.deleteTempDirectory(second);
        }
    }

    @Test
    public void shouldDeleteTheTemporaryDirectoryWithItsContent() throws IOException {
        Path tempDir = this.logServlet.createPrivateTempDirectory();
        Files.write(tempDir.resolve("kura_journal.log"), "journal".getBytes(StandardCharsets.UTF_8));
        Files.createDirectory(tempDir.resolve("nested"));
        Files.write(tempDir.resolve("nested").resolve("Kura_Logs.zip"), new byte[] { 1, 2, 3 });

        this.logServlet.deleteTempDirectory(tempDir);

        assertFalse(Files.exists(tempDir));
    }

    @Test
    public void shouldTolerateANullOrAlreadyDeletedTemporaryDirectory() throws IOException {
        Path tempDir = this.logServlet.createPrivateTempDirectory();
        Files.delete(tempDir);

        this.logServlet.deleteTempDirectory(null);
        this.logServlet.deleteTempDirectory(tempDir);

        assertFalse(Files.exists(tempDir));
    }

    /*
     * utilities
     */

    private File givenFile(File parent, String name, String content) throws IOException {
        File file = new File(parent, name);
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private Map<String, String> whenFilesAreZipped(List<File> files) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            this.logServlet.zipFiles(zos, files);
        }

        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                ByteArrayOutputStream entryContent = new ByteArrayOutputStream();
                byte[] buffer = new byte[64];
                int read;
                while ((read = zis.read(buffer)) != -1) {
                    entryContent.write(buffer, 0, read);
                }
                entries.put(entry.getName(), new String(entryContent.toByteArray(), StandardCharsets.UTF_8));
            }
        }

        return entries;
    }
}
