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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.Security;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPBEEncryptedData;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder;
import org.bouncycastle.util.io.Streams;
import org.eclipse.kura.KuraIOException;
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

    protected static void decryptModel(String password, String inputFilePath, String outputFilePath)
            throws IOException, KuraIOException {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        if (!Files.isRegularFile(Paths.get(inputFilePath))) {
            throw new IOException("Input file " + inputFilePath + " does not exists/is not a file");
        }

        if (Files.exists(Paths.get(outputFilePath))) {
            throw new IOException("Output file " + outputFilePath + " already exists");
        }

        try {
            decryptFile(password, inputFilePath, outputFilePath);
        } catch (PGPException e) {
            throw new KuraIOException(e, "File decryption failed.");
        }
    }

    protected static void unzipModel(String inputFilePath, String outputFolder) {
        // TODO
    }

    protected static void deleteModel(String modelRootPath) throws IOException {
        if (!Files.exists(Paths.get(modelRootPath))) {
            throw new IOException("Model root folder " + modelRootPath + " does not exists");
        }

        FileUtils.cleanDirectory(new File(modelRootPath));
        Files.delete(Paths.get(modelRootPath));
    }

    private static void decryptFile(String password, String inputFilePath, String outputFilePath)
            throws IOException, PGPException {
        InputStream inStream = new BufferedInputStream(new FileInputStream(inputFilePath));

        inStream = PGPUtil.getDecoderStream(inStream);

        JcaPGPObjectFactory pgpFactory = new JcaPGPObjectFactory(inStream);
        Object currentObj = pgpFactory.nextObject();

        PGPEncryptedDataList enc;
        if (currentObj instanceof PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList) currentObj;
        } else {
            enc = (PGPEncryptedDataList) pgpFactory.nextObject();
        }

        PGPPBEEncryptedData pbe = (PGPPBEEncryptedData) enc.get(0);
        InputStream clear = pbe.getDataStream(new JcePBEDataDecryptorFactoryBuilder(
                new JcaPGPDigestCalculatorProviderBuilder().setProvider("BC").build()).setProvider("BC")
                        .build(password.toCharArray()));

        pgpFactory = new JcaPGPObjectFactory(clear);
        currentObj = pgpFactory.nextObject();

        if (currentObj instanceof PGPCompressedData) {
            PGPCompressedData compressedData = (PGPCompressedData) currentObj;
            pgpFactory = new JcaPGPObjectFactory(compressedData.getDataStream());
            currentObj = pgpFactory.nextObject();
        }

        PGPLiteralData literalData = (PGPLiteralData) currentObj;
        InputStream decryptedStream = literalData.getInputStream();

        OutputStream outStream = new FileOutputStream(outputFilePath);
        Streams.pipeAll(decryptedStream, outStream);
        outStream.close();
        inStream.close();

        if (pbe.isIntegrityProtected()) {
            if (!pbe.verify()) {
                logger.error("File failed integrity check");
            } else {
                logger.info("File integrity check passed");
            }
        } else {
            logger.info("No file integrity check");
        }
    }
}
