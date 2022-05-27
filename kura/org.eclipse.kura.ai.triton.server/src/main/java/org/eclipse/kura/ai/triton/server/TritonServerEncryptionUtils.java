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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.Security;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
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
import org.eclipse.kura.util.zip.UnZip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TritonServerEncryptionUtils {

    private static final Logger logger = LoggerFactory.getLogger(TritonServerEncryptionUtils.class);

    private TritonServerEncryptionUtils() {
    }

    protected static String getEncryptedModelPath(String modelName, String folderPath) throws KuraIOException {
        if (!Files.isDirectory(Paths.get(folderPath))) {
            throw new KuraIOException("Model repository folder " + folderPath + " does not exist/is not a folder");
        }

        File dir = new File(folderPath);
        FileFilter fileFilter = new WildcardFileFilter(modelName + ".*");
        File[] files = dir.listFiles(fileFilter);

        if (files.length != 1) {
            throw new KuraIOException("No good match found in folder path");
        }

        return files[0].toString();
    }

    protected static String createDecryptionFolder(String prefix) throws IOException {
        Set<PosixFilePermission> permissions = new HashSet<>(Arrays.asList(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        Path tempFolderPath = Files.createTempDirectory(prefix, PosixFilePermissions.asFileAttribute(permissions));

        logger.debug("Created temporary directory at path {}", tempFolderPath);

        return tempFolderPath.toString();
    }

    protected static void decryptModel(String password, String inputFilePath, String outputFilePath)
            throws IOException, KuraIOException {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        if (!Files.isRegularFile(Paths.get(inputFilePath))) {
            throw new IOException("Input file " + inputFilePath + " does not exist/is not a file");
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

    protected static void unzipModel(String inputFilePath, String outputFolder) throws IOException {
        if (!Files.isRegularFile(Paths.get(inputFilePath))) {
            throw new IOException("Input file " + inputFilePath + " does not exist/is not a file");
        }

        if (!UnZip.isZipCompressed(inputFilePath)) {
            throw new IOException("ZIP magic number check failed. Wrong file format");
        }

        if (!Files.isDirectory(Paths.get(outputFolder))) {
            throw new IOException("Output folder " + outputFolder + " does not exist/is not a folder");
        }

        UnZip.unZipFile(inputFilePath, outputFolder);
    }

    protected static void cleanRepository(String modelRootPath) {
        if (!Files.isDirectory(Paths.get(modelRootPath))) {
            logger.warn("Model root folder {} does not exist", modelRootPath);
            return;
        }

        try {
            FileUtils.cleanDirectory(new File(modelRootPath));
        } catch (IOException e) {
            logger.warn("Cannot clean directory at path {}", modelRootPath, e);
        }
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

        if (enc == null) {
            inStream.close();
            throw new IOException("PGP PBE File format read failed");
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
