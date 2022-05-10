package org.eclipse.kura.ai.triton.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.junit.Test;

public class TritonServerEncryptionUtilsTest {

    /*
     * Scenarios
     */

    @Test
    public void createDecryptionFolderShouldWork() {
        final String TARGET_FOLDER = "target_folder";
        Path targetFolderPath = Paths.get(TARGET_FOLDER);

        // Given: Target folder should not exists
        assertFalse(Files.exists(targetFolderPath));

        // When: Run the createDecryptionFolder method
        try {
            TritonServerEncryptionUtils.createDecryptionFolder(TARGET_FOLDER);
        } catch (IOException e1) {
            e1.printStackTrace();
            assertTrue(false);
        }

        // Then: Folder should exists
        assertTrue(Files.isDirectory(targetFolderPath));

        // Then: Folder should have the expected permissions
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(targetFolderPath,
                    LinkOption.NOFOLLOW_LINKS);
            Set<PosixFilePermission> expectedPermissions = PosixFilePermissions.fromString("rwx------");

            assertEquals(expectedPermissions, permissions);
        } catch (IOException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        // Cleanup
        try {
            Files.delete(targetFolderPath);
        } catch (IOException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void createDecryptionFolderShouldThrowOnNameClashes() {
        final String TARGET_FOLDER = "another_folder";
        Path targetFolderPath = Paths.get(TARGET_FOLDER);

        // Given: A file exists at target path
        try {
            Files.createFile(targetFolderPath);
        } catch (IOException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        assertTrue(Files.exists(targetFolderPath));

        // When: Run the createDecryptionFolder method
        try {
            TritonServerEncryptionUtils.createDecryptionFolder(TARGET_FOLDER);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        // Then: An exception was thrown
        // TODO

        // Cleanup
        try {
            Files.delete(targetFolderPath);
        } catch (IOException e) {
            e.printStackTrace();
            assertTrue(false);
        }

    }

    /*
     * Steps
     */

    /*
     * Given
     */
    // TODO

    /*
     * When
     */
    // TODO

    /*
     * Then
     */
    // TODO
}
