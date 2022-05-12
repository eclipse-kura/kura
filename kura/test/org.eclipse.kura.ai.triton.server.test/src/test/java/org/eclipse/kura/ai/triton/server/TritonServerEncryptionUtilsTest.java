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

    /*
     * Scenarios
     */

    @Test
    public void createDecryptionFolderShouldWork() {
        givenTargetFolder(WORKDIR + "/target_folder");
        givenNoFileExistsAtPath(this.targetFolder);

        whenCreateDecryptionFolderIsCalledWith(this.targetFolder);

        thenTargetFolderExists(this.targetFolder);
        thenTargetFolderHasPermissions(this.targetFolder, "rwx------");
        thenNoExceptionOccurred();
    }

    @Test
    public void createDecryptionFolderShouldWorkWithNestedPath() {
        givenTargetFolder(WORKDIR + "/new/nested/folder");
        givenNoFileExistsAtPath(this.targetFolder);

        whenCreateDecryptionFolderIsCalledWith(this.targetFolder);

        thenTargetFolderExists(this.targetFolder);
        thenTargetFolderHasPermissions(this.targetFolder, "rwx------");
        thenNoExceptionOccurred();
    }

    @Test
    public void createDecryptionFolderShouldThrowOnNameClashes() {
        givenTargetFolder(WORKDIR + "/another_folder");
        givenAFileExistsAtPath(this.targetFolder);

        whenCreateDecryptionFolderIsCalledWith(this.targetFolder);

        thenAnExceptionOccurred();
    }

    @Test
    public void decryptModelShouldWork() {
        givenEncryptedFileAtPath("target/test-classes/plain_file.gpg");
        givenAnEncryptedFileExistsAtPath(this.encryptedFile);
        givenDecryptedFileAtPath(WORKDIR + "/file");
        givenNoFileExistsAtPath(this.decryptedFile);

        whenDecryptModelIsCalledWith("password", this.encryptedFile, this.decryptedFile);

        thenFileExistsAtPath(this.decryptedFile);
        thenDecryptedFileContentMatches(this.decryptedFile, "cudumar");
        thenNoExceptionOccurred();
    }

    @Test
    public void decryptModelShouldWorkWithZippedFiles() {
        // Given: encrypted file at path
        givenEncryptedFileAtPath("target/test-classes/plain.txt.zip.gpg");
        givenAnEncryptedFileExistsAtPath(this.encryptedFile);
        givenDecryptedFileAtPath(WORKDIR + "/file");
        givenNoFileExistsAtPath(this.decryptedFile);

        whenDecryptModelIsCalledWith("secret", this.encryptedFile, this.decryptedFile);

        thenFileExistsAtPath(this.decryptedFile);
        thenNoExceptionOccurred();
    }

    @Test
    public void decryptModelShouldWorkWithASCIIArmoredFormat() {
        givenEncryptedFileAtPath("target/test-classes/armored_plain_file.asc");
        givenAnEncryptedFileExistsAtPath(this.encryptedFile);
        givenDecryptedFileAtPath(WORKDIR + "/file");
        givenNoFileExistsAtPath(this.decryptedFile);

        whenDecryptModelIsCalledWith("eurotech", this.encryptedFile, this.decryptedFile);

        thenFileExistsAtPath(this.decryptedFile);
        thenDecryptedFileContentMatches(this.decryptedFile, "42");
        thenNoExceptionOccurred();
    }

    @Test
    public void decryptModelShouldThrowWithWrongPassword() {
        givenEncryptedFileAtPath("target/test-classes/armored_plain_file.asc");
        givenAnEncryptedFileExistsAtPath(this.encryptedFile);
        givenDecryptedFileAtPath(WORKDIR + "/file");
        givenNoFileExistsAtPath(this.decryptedFile);

        whenDecryptModelIsCalledWith("wrongpassword", this.encryptedFile, this.decryptedFile);

        thenAnExceptionOccurred();
        thenFileDoesNotExistsAtPath(this.decryptedFile);
    }

    @Test
    public void decryptModelShouldThrowIfDestinationFileAlreadyExists() {
        givenEncryptedFileAtPath("target/test-classes/armored_plain_file.asc");
        givenAnEncryptedFileExistsAtPath(this.encryptedFile);
        givenDecryptedFileAtPath(WORKDIR + "/file");
        givenAFileExistsAtPath(this.decryptedFile);

        whenDecryptModelIsCalledWith("eurotech", this.encryptedFile, this.decryptedFile);

        thenAnExceptionOccurred();
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

    private void givenAnEncryptedFileExistsAtPath(String filePath) {
        Path targetFilePath = Paths.get(filePath);
        assertTrue(Files.exists(targetFilePath));
        assertTrue(Files.isRegularFile(targetFilePath));
    }

    private void givenAFileExistsAtPath(String folderPath) {
        Path targetFolderPath = Paths.get(folderPath);
        assertFalse(Files.exists(targetFolderPath));

        try {
            Files.createFile(targetFolderPath);
        } catch (IOException e) {
            this.exceptionOccurred = true;
        }

        assertTrue(Files.exists(targetFolderPath));
    }

    private void givenNoFileExistsAtPath(String folderPath) {
        Path targetFolderPath = Paths.get(folderPath);
        assertFalse(Files.exists(targetFolderPath));
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

    /*
     * Then
     */
    private void thenFileExistsAtPath(String filePath) {
        Path targetFilePath = Paths.get(filePath);
        assertTrue(Files.isRegularFile(targetFilePath));
    }

    private void thenFileDoesNotExistsAtPath(String filePath) {
        Path targetFilePath = Paths.get(filePath);
        assertFalse(Files.exists(targetFilePath));
    }

    private void thenTargetFolderExists(String folderPath) {
        Path targetFolderPath = Paths.get(folderPath);
        assertTrue(Files.isDirectory(targetFolderPath));
    }

    private void thenTargetFolderHasPermissions(String folderPath, String permissions) {
        Path targetFolderPath = Paths.get(folderPath);
        try {
            Set<PosixFilePermission> readPermissions = Files.getPosixFilePermissions(targetFolderPath,
                    LinkOption.NOFOLLOW_LINKS);
            Set<PosixFilePermission> expectedPermissions = PosixFilePermissions.fromString(permissions);

            assertEquals(expectedPermissions, readPermissions);
        } catch (IOException e) {
            this.exceptionOccurred = true;
        }
    }

    private void thenDecryptedFileContentMatches(String filePath, String expectedContent) {
        Path targetFilePath = Paths.get(filePath);
        try {
            List<String> content = Files.readAllLines(targetFilePath);

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
        Path workdirPath = Paths.get(WORKDIR);
        try {
            Files.createDirectories(workdirPath);
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
