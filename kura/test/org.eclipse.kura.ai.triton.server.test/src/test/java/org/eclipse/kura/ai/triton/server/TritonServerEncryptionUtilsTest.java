/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/

package org.eclipse.kura.ai.triton.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraIOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TritonServerEncryptionUtilsTest {

    private static final String WORKDIR = System.getProperty("java.io.tmpdir") + "/decr_folder";
    private boolean exceptionOccurred = false;
    private String targetFolder;
    private String encryptedFile;
    private String decryptedFile;
    private String compressedFile;

    /*
     * Scenarios
     */

    @Test
    public void createDecryptionFolderShouldWork() {
        givenTargetFolder(WORKDIR + "/target_folder");
        givenNoFileExistsAtPath(targetFolder);

        whenCreateDecryptionFolderIsCalledWith(targetFolder);

        thenAFolderExistsAtPath(targetFolder);
        thenTargetFolderHasPermissions(targetFolder, "rwx------");
        thenNoExceptionOccurred();
    }

    @Test
    public void createDecryptionFolderShouldWorkWithNestedPath() {
        givenTargetFolder(WORKDIR + "/new/nested/folder");
        givenNoFileExistsAtPath(targetFolder);

        whenCreateDecryptionFolderIsCalledWith(targetFolder);

        thenAFolderExistsAtPath(targetFolder);
        thenTargetFolderHasPermissions(targetFolder, "rwx------");
        thenNoExceptionOccurred();
    }

    @Test
    public void createDecryptionFolderShouldThrowOnNameClashes() {
        givenTargetFolder(WORKDIR + "/another_folder");
        givenAFileAreadyExistsAtPath(targetFolder);

        whenCreateDecryptionFolderIsCalledWith(targetFolder);

        thenAnExceptionOccurred();
    }

    @Test
    public void decryptModelShouldWork() {
        givenEncryptedFileAtPath("target/test-classes/plain_file.gpg");
        givenAFileExistsAtPath(encryptedFile);
        givenDecryptedFileAtPath(WORKDIR + "/file");
        givenNoFileExistsAtPath(decryptedFile);

        whenDecryptModelIsCalledWith("password", encryptedFile, decryptedFile);

        thenFileExistsAtPath(decryptedFile);
        thenFileContentMatches(decryptedFile, "cudumar");
        thenNoExceptionOccurred();
    }

    @Test
    public void decryptModelShouldWorkWithZippedFiles() {
        givenEncryptedFileAtPath("target/test-classes/plain.txt.zip.gpg");
        givenAFileExistsAtPath(encryptedFile);
        givenDecryptedFileAtPath(WORKDIR + "/file");
        givenNoFileExistsAtPath(decryptedFile);

        whenDecryptModelIsCalledWith("secret", encryptedFile, decryptedFile);

        thenFileExistsAtPath(decryptedFile);
        thenNoExceptionOccurred();
    }

    @Test
    public void decryptModelShouldWorkWithASCIIArmoredFormat() {
        givenEncryptedFileAtPath("target/test-classes/armored_plain_file.asc");
        givenAFileExistsAtPath(encryptedFile);
        givenDecryptedFileAtPath(WORKDIR + "/file");
        givenNoFileExistsAtPath(decryptedFile);

        whenDecryptModelIsCalledWith("eurotech", encryptedFile, decryptedFile);

        thenFileExistsAtPath(decryptedFile);
        thenFileContentMatches(decryptedFile, "42");
        thenNoExceptionOccurred();
    }

    @Test
    public void decryptModelShouldThrowWithWrongPassword() {
        givenEncryptedFileAtPath("target/test-classes/armored_plain_file.asc");
        givenAFileExistsAtPath(encryptedFile);
        givenDecryptedFileAtPath(WORKDIR + "/file");
        givenNoFileExistsAtPath(decryptedFile);

        whenDecryptModelIsCalledWith("wrongpassword", encryptedFile, decryptedFile);

        thenAnExceptionOccurred();
        thenFileDoesNotExistsAtPath(decryptedFile);
    }

    @Test
    public void decryptModelShouldThrowWithWrongFileFormat() {
        givenEncryptedFileAtPath("target/test-classes/tf_autoencoder_fp32.zip");
        givenAFileExistsAtPath(encryptedFile);
        givenDecryptedFileAtPath(WORKDIR + "/file");
        givenNoFileExistsAtPath(decryptedFile);

        whenDecryptModelIsCalledWith("anything", encryptedFile, decryptedFile);

        thenAnExceptionOccurred();
        thenFileDoesNotExistsAtPath(decryptedFile);
    }

    @Test
    public void decryptModelShouldThrowIfDestinationFileAlreadyExists() {
        givenEncryptedFileAtPath("target/test-classes/armored_plain_file.asc");
        givenAFileExistsAtPath(encryptedFile);
        givenDecryptedFileAtPath(WORKDIR + "/file");
        givenAFileAreadyExistsAtPath(decryptedFile);

        whenDecryptModelIsCalledWith("eurotech", encryptedFile, decryptedFile);

        thenAnExceptionOccurred();
    }

    @Test
    public void unzipModelShouldWork() {
        givenCompressedFileAtPath("target/test-classes/tf_autoencoder_fp32.zip");
        givenAFileExistsAtPath(compressedFile);
        givenTargetFolder(WORKDIR);
        givenAFolderExistsAtPath(targetFolder);
        givenFolderAtPathIsEmpty(targetFolder);

        whenUnzipModelIsCalledWith(compressedFile, targetFolder);

        thenAFolderExistsAtPath(targetFolder + "/tf_autoencoder_fp32");
        thenFileExistsAtPath(targetFolder + "/tf_autoencoder_fp32/config.pbtxt");
        thenFileContentMatches(targetFolder + "/tf_autoencoder_fp32/config.pbtxt", "test");
        thenAFolderExistsAtPath(targetFolder + "/tf_autoencoder_fp32/1");
        thenAFolderExistsAtPath(targetFolder + "/tf_autoencoder_fp32/1/model.savedmodel");
        thenFileExistsAtPath(targetFolder + "/tf_autoencoder_fp32/1/model.savedmodel/saved_model.pb");
        thenFileContentMatches(targetFolder + "/tf_autoencoder_fp32/1/model.savedmodel/saved_model.pb",
                "model");
        thenNoExceptionOccurred();
    }

    @Test
    public void unzipModelShouldThrowIfFileDoesNotExist() {
        givenCompressedFileAtPath(WORKDIR + "/non_existent_file.zip");
        givenNoFileExistsAtPath(compressedFile);
        givenTargetFolder(WORKDIR);
        givenAFolderExistsAtPath(targetFolder);
        givenFolderAtPathIsEmpty(targetFolder);

        whenUnzipModelIsCalledWith(compressedFile, targetFolder);

        thenAnExceptionOccurred();
        thenFolderIsEmpty(targetFolder);
    }

    @Test
    public void unzipModelShouldThrowWithWrongFileFormat() {
        givenCompressedFileAtPath("target/test-classes/armored_plain_file.asc");
        givenAFileExistsAtPath(compressedFile);
        givenTargetFolder(WORKDIR);
        givenAFolderExistsAtPath(targetFolder);
        givenFolderAtPathIsEmpty(targetFolder);

        whenUnzipModelIsCalledWith(compressedFile, targetFolder);

        thenAnExceptionOccurred();
        thenFolderIsEmpty(targetFolder);
    }

    @Test
    public void unzipModelShouldThrowIfDestinationFolderDoesNotExist() {
        givenCompressedFileAtPath("target/test-classes/armored_plain_file.asc");
        givenAFileExistsAtPath(compressedFile);
        givenTargetFolder(WORKDIR + "/an_imaginary_folder");
        givenNoFileExistsAtPath(targetFolder);

        whenUnzipModelIsCalledWith(compressedFile, targetFolder);

        thenAnExceptionOccurred();
        thenFileDoesNotExistsAtPath(targetFolder);
    }

    @Test
    public void deleteModelShouldWork() {
        givenTargetFolder(WORKDIR + "/model_dir");
        givenAFolderAreadyExistsAtPath(targetFolder);
        givenAFileAreadyExistsAtPath(targetFolder + "/test_file1");
        givenAFileAreadyExistsAtPath(targetFolder + "/test_file2");

        whenDeleteModelIsCalledWith(targetFolder);

        thenFileDoesNotExistsAtPath(targetFolder);
        thenAFolderExistsAtPath(WORKDIR);
        thenNoExceptionOccurred();
    }

    @Test
    public void deleteModelShouldThrowWithNonExistingFolder() {
        givenTargetFolder(WORKDIR + "/model_dir");
        givenNoFileExistsAtPath(targetFolder);

        whenDeleteModelIsCalledWith(targetFolder);

        thenAnExceptionOccurred();
        thenAFolderExistsAtPath(WORKDIR);
    }

    /*
     * Steps
     */

    /*
     * Given
     */
    private void givenTargetFolder(String folderPath) {
        this.targetFolder = folderPath;
    }

    private void givenEncryptedFileAtPath(String filePath) {
        this.encryptedFile = filePath;
    }

    private void givenDecryptedFileAtPath(String filePath) {
        this.decryptedFile = filePath;
    }

    private void givenCompressedFileAtPath(String filePath) {
        this.compressedFile = filePath;
    }

    private void givenAFileExistsAtPath(String filePath) {
        Path targetFilePath = Paths.get(filePath);
        assertTrue(Files.exists(targetFilePath));
        assertTrue(Files.isRegularFile(targetFilePath));
    }

    private void givenAFolderExistsAtPath(String folderPath) {
        Path targetPath = Paths.get(folderPath);
        assertTrue(Files.exists(targetPath));
        assertTrue(Files.isDirectory(targetPath));
    }

    private void givenFolderAtPathIsEmpty(String folderPath) {
        try (Stream<Path> entries = Files.list(Paths.get(targetFolder))) {
            assertFalse(entries.findFirst().isPresent());
        } catch (IOException e) {
            e.printStackTrace();
            this.exceptionOccurred = true;
        }
    }

    private void givenNoFileExistsAtPath(String folderPath) {
        assertFalse(Files.exists(Paths.get(folderPath)));
    }

    private void givenAFolderAreadyExistsAtPath(String folderPath) {
        Path targetFolderPath = Paths.get(folderPath);
        assertFalse(Files.exists(targetFolderPath));

        try {
            Files.createDirectory(targetFolderPath);
        } catch (IOException e) {
            this.exceptionOccurred = true;
        }

        assertTrue(Files.isDirectory(targetFolderPath));
    }

    private void givenAFileAreadyExistsAtPath(String folderPath) {
        Path targetFolderPath = Paths.get(folderPath);
        assertFalse(Files.exists(targetFolderPath));

        try {
            Files.createFile(targetFolderPath);
        } catch (IOException e) {
            this.exceptionOccurred = true;
        }

        assertTrue(Files.isRegularFile(targetFolderPath));
    }

    /*
     * When
     */
    private void whenCreateDecryptionFolderIsCalledWith(String folderPath) {
        try {
            TritonServerEncryptionUtils.createDecryptionFolder(folderPath);
        } catch (IOException e) {
            this.exceptionOccurred = true;
        }
    }

    private void whenDecryptModelIsCalledWith(String password, String encryptedFilePath, String decryptedFilePath) {
        try {
            TritonServerEncryptionUtils.decryptModel(password, encryptedFilePath, decryptedFilePath);
        } catch (IOException | KuraIOException e) {
            e.printStackTrace();
            this.exceptionOccurred = true;
        }
    }

    private void whenUnzipModelIsCalledWith(String compressedFilePath, String folderPath) {
        try {
            TritonServerEncryptionUtils.unzipModel(compressedFilePath, folderPath);
        } catch (IOException e1) {
            e1.printStackTrace();
            this.exceptionOccurred = true;
        }
    }

    private void whenDeleteModelIsCalledWith(String folderPath) {
        try {
            TritonServerEncryptionUtils.deleteModel(folderPath);
        } catch (IOException e) {
            e.printStackTrace();
            this.exceptionOccurred = true;
        }
    }

    /*
     * Then
     */
    private void thenFileExistsAtPath(String filePath) {
        assertTrue(Files.isRegularFile(Paths.get(filePath)));
    }

    private void thenFileDoesNotExistsAtPath(String filePath) {
        assertFalse(Files.exists(Paths.get(filePath)));
    }

    private void thenAFolderExistsAtPath(String folderPath) {
        assertTrue(Files.isDirectory(Paths.get(folderPath)));
    }

    private void thenTargetFolderHasPermissions(String folderPath, String permissions) {
        try {
            Set<PosixFilePermission> readPermissions = Files.getPosixFilePermissions(Paths.get(folderPath),
                    LinkOption.NOFOLLOW_LINKS);
            Set<PosixFilePermission> expectedPermissions = PosixFilePermissions.fromString(permissions);

            assertEquals(expectedPermissions, readPermissions);
        } catch (IOException e) {
            this.exceptionOccurred = true;
        }
    }

    private void thenFolderIsEmpty(String folderPath) {
        try (Stream<Path> entries = Files.list(Paths.get(targetFolder))) {
            assertFalse(entries.findFirst().isPresent());
        } catch (IOException e) {
            e.printStackTrace();
            this.exceptionOccurred = true;
        }
    }

    private void thenFileContentMatches(String filePath, String expectedContent) {
        try {
            List<String> content = Files.readAllLines(Paths.get(filePath));

            assertEquals(1, content.size());
            assertEquals(expectedContent, content.get(0));
        } catch (IOException e) {
            e.printStackTrace();
            this.exceptionOccurred = true;
        }
    }

    private void thenNoExceptionOccurred() {
        assertFalse(this.exceptionOccurred);
    }

    private void thenAnExceptionOccurred() {
        assertTrue(this.exceptionOccurred);
    }

    /*
     * Cleanup
     */
    @Before
    public void setup() {
        try {
            Files.createDirectories(Paths.get(WORKDIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @After
    public void teardown() {
        try {
            FileUtils.cleanDirectory(new File(WORKDIR));
            Files.delete(Paths.get(WORKDIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
