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
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TritonServerEncryptionUtilsTest {

    private static final String WORKDIR = "./decr_folder";
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
        String encryptedFile = "target/test-classes/empty_file.asc";
        Path encryptedFilePath = Paths.get(encryptedFile);

        assertTrue(Files.exists(encryptedFilePath));

        String decryptedFile = "target/test-classes/file";
        Path decryptedFilePath = Paths.get(decryptedFile);

        assertFalse(Files.exists(decryptedFilePath));

        try {
            TritonServerEncryptionUtils.decryptModel("eurotech", encryptedFile, decryptedFile);
        } catch (IOException e1) {
            e1.printStackTrace();
            this.exceptionOccurred = true;
        }
        assertFalse(this.exceptionOccurred);

        assertTrue(Files.exists(decryptedFilePath));

        try {
            List<String> content = Files.readAllLines(decryptedFilePath);

            assertEquals(content.size(), 1);
            assertEquals(content.get(0), "42");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void decryptModelShouldThrowWithWrongPassword() {
        String encryptedFile = "target/test-classes/empty_file.asc";
        Path encryptedFilePath = Paths.get(encryptedFile);

        assertTrue(Files.exists(encryptedFilePath));

        String decryptedFile = "target/test-classes/file";
        Path decryptedFilePath = Paths.get(decryptedFile);

        assertFalse(Files.exists(decryptedFilePath));

        try {
            TritonServerEncryptionUtils.decryptModel("wrongpassword", encryptedFile, decryptedFile);
        } catch (IOException e) {
            e.printStackTrace();
            this.exceptionOccurred = true;
        }
        assertTrue(this.exceptionOccurred);

        assertFalse(Files.exists(decryptedFilePath));
    }

    @Test
    public void decryptModelShouldThrowIfDestinationFileAlreadyExists() {
        String encryptedFile = "target/test-classes/empty_file.asc";
        Path encryptedFilePath = Paths.get(encryptedFile);

        assertTrue(Files.exists(encryptedFilePath));

        String decryptedFile = "target/test-classes/file";
        Path decryptedFilePath = Paths.get(decryptedFile);

        try {
            Files.createFile(decryptedFilePath);
        } catch (IOException e) {
            this.exceptionOccurred = true;
        }
        assertFalse(this.exceptionOccurred);

        assertTrue(Files.exists(decryptedFilePath));

        try {
            TritonServerEncryptionUtils.decryptModel("eurotech", encryptedFile, decryptedFile);
        } catch (IOException e) {
            e.printStackTrace();
            this.exceptionOccurred = true;
        }
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
        Path workdirPath = Paths.get(WORKDIR);
        try {
            Files.walk(workdirPath).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
