package org.eclipse.kura.ai.triton.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TritonServerEncryptionUtils {

    private static final Logger logger = LoggerFactory.getLogger(TritonServerEncryptionUtils.class);

    private TritonServerEncryptionUtils() {
        // TODO
    }

    protected static void createDecryptionFolder(String folderPath) throws IOException {
        Path targetFolderPath = Paths.get(folderPath);

        if (Files.exists(targetFolderPath)) {
            throw new IOException("Target path " + targetFolderPath.toString() + " already exists");
        }

        logger.debug("Creating decryption folder at path: {}", folderPath);

        Files.createDirectories(targetFolderPath);

        Set<PosixFilePermission> permissions = new HashSet<>(Arrays.asList(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        Files.setPosixFilePermissions(targetFolderPath, permissions);
    }

    protected static void decryptModel(String password, String inputFilePath, String outputFolder) {
        // TODO
    }

    protected static void unzipModel(String inputFilePath, String outputFolder) {
        // TODO
    }

    protected static void deleteModel(String modelName) {
        // TODO
    }
}
