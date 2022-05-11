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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TritonServerEncryptionUtilsTest {

    private static final String WORKDIR = System.getProperty("java.io.tmpdir") + "/decr_folder";
    private boolean exceptionOccurred = false;
    private String targetFolder;

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
        // Given: encrypted file at path
        String encryptedFile = "target/test-classes/another_file.gpg";
        Path encryptedFilePath = Paths.get(encryptedFile);

        assertTrue(Files.exists(encryptedFilePath));

        // Given: decrypted file at path
        String decryptedFile = WORKDIR + "/file";
        Path decryptedFilePath = Paths.get(decryptedFile);

        assertFalse(Files.exists(decryptedFilePath));

        // When: decryptModel is called with params
        try {
            TritonServerEncryptionUtils.decryptModel("password", encryptedFile, decryptedFile);
        } catch (IOException e) {
            e.printStackTrace();
            this.exceptionOccurred = true;
        }

        // Then: the decrypted file exists
        assertTrue(Files.exists(decryptedFilePath));

        // Then: the decrypted file content matches expectations
        try {
            List<String> content = Files.readAllLines(decryptedFilePath);

            assertEquals(content.size(), 1);
            assertEquals(content.get(0), "cudumar");
        } catch (IOException e) {
            e.printStackTrace();
            this.exceptionOccurred = true;
        }

        // Then: no exception occurred
        assertFalse(this.exceptionOccurred);
    }

    @Test
    public void decryptModelShouldWorkWithASCIIArmoredFormat() {
        // Given: encrypted file at path
        String encryptedFile = "target/test-classes/empty_file.asc";
        Path encryptedFilePath = Paths.get(encryptedFile);

        assertTrue(Files.exists(encryptedFilePath));

        // Given: decrypted file at path
        String decryptedFile = WORKDIR + "/file";
        Path decryptedFilePath = Paths.get(decryptedFile);

        assertFalse(Files.exists(decryptedFilePath));

        // When: decryptModel is called with params
        try {
            TritonServerEncryptionUtils.decryptModel("eurotech", encryptedFile, decryptedFile);
        } catch (IOException e1) {
            e1.printStackTrace();
            this.exceptionOccurred = true;
        }

        // Then: the decrypted file exists
        assertTrue(Files.exists(decryptedFilePath));

        // Then: the decrypted file content matches expectations
        try {
            List<String> content = Files.readAllLines(decryptedFilePath);

            assertEquals(content.size(), 1);
            assertEquals(content.get(0), "42");
        } catch (IOException e) {
            e.printStackTrace();
            this.exceptionOccurred = true;
        }

        // Then: no exception occurred
        assertFalse(this.exceptionOccurred);
    }

    @Test
    public void decryptModelShouldThrowWithWrongPassword() {
        // Given: encrypted file at path
        String encryptedFile = "target/test-classes/empty_file.asc";
        Path encryptedFilePath = Paths.get(encryptedFile);

        assertTrue(Files.exists(encryptedFilePath));

        // Given: decrypted file at path
        String decryptedFile = WORKDIR + "/file";
        Path decryptedFilePath = Paths.get(decryptedFile);

        assertFalse(Files.exists(decryptedFilePath));

        // When: decryptModel is called with params
        try {
            TritonServerEncryptionUtils.decryptModel("wrongpassword", encryptedFile, decryptedFile);
        } catch (IOException e) {
            e.printStackTrace();
            this.exceptionOccurred = true;
        }

        // Then: decrypted file doesn't exists
        assertFalse(Files.exists(decryptedFilePath));

        // Then: an exception occurred
        assertTrue(this.exceptionOccurred);

    }

    @Test
    public void decryptModelShouldThrowIfDestinationFileAlreadyExists() {
        // Given: encrypted file at path
        String encryptedFile = "target/test-classes/empty_file.asc";
        Path encryptedFilePath = Paths.get(encryptedFile);

        assertTrue(Files.exists(encryptedFilePath));

        // Given: decrypted file at path
        String decryptedFile = WORKDIR + "/file";
        Path decryptedFilePath = Paths.get(decryptedFile);

        // Given: a file at decryptedFilePath already exists
        try {
            Files.createFile(decryptedFilePath);
        } catch (IOException e) {
            this.exceptionOccurred = true;
        }
        assertFalse(this.exceptionOccurred);

        assertTrue(Files.exists(decryptedFilePath));

        // When: decryptModel is called with params
        try {
            TritonServerEncryptionUtils.decryptModel("eurotech", encryptedFile, decryptedFile);
        } catch (IOException e) {
            e.printStackTrace();
            this.exceptionOccurred = true;
        }

        // Then: an exception occurred
        assertTrue(this.exceptionOccurred);
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

    /*
     * Then
     */
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
