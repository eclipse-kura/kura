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
 *******************************************************************************/
package org.eclipse.kura.util.zip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

public class UnZipTest {

    private static final String WORK_FOLDER = "/tmp/kura_test/";
    private static final String SINGLE_INPUT_ZIP_FILE = "singleCompressedFile.zip";
    private static final String MULTIPLE_INPUT_ZIP_FILE = "multipleCompressedFile.zip";
    private static final String TOO_MANY_INPUT_ZIP_FILE = "tooManyCompressedFiles.zip";
    private static final String TOO_BIG_INPUT_ZIP_FILE = "tooBigCompressedFile.zip";
    private static final String ILLEGAL_PATH_INPUT_ZIP_FILE = "illegalPathCompressedFile.zip";
    private static final String UNCOMPRESSED_INPUT_ZIP_FILE = "uncompressedFile.txt";
    private static final String INPUT_TAR_FILE = "singleCompressedFile.tar";
    private static final String SINGLE_INPUT_ZIP_FILE_PATH = "target/test-classes/" + SINGLE_INPUT_ZIP_FILE;
    private static final String MULTIPLE_INPUT_ZIP_FILE_PATH = "target/test-classes/" + MULTIPLE_INPUT_ZIP_FILE;
    private static final String TOO_MANY_INPUT_ZIP_FILE_PATH = "target/test-classes/" + TOO_MANY_INPUT_ZIP_FILE;
    private static final String TOO_BIG_INPUT_ZIP_FILE_PATH = "target/test-classes/" + TOO_BIG_INPUT_ZIP_FILE;
    private static final String ILLEGAL_PATH_INPUT_ZIP_FILE_PATH = "target/test-classes/" + ILLEGAL_PATH_INPUT_ZIP_FILE;
    private static final String UNCOMPRESSED_INPUT_ZIP_FILE_PATH = "target/test-classes/" + UNCOMPRESSED_INPUT_ZIP_FILE;
    private static final String INPUT_TAR_FILE_PATH = "target/test-classes/" + INPUT_TAR_FILE;

    private byte[] compressedInput;
    private boolean exceptionCaught;

    @After
    public void clean() throws IOException {
        FileUtils.deleteDirectory(new File(WORK_FOLDER));
    }

    @Test
    public void unZipFileTest() throws IOException {
        givenCompressedFile();

        whenFileIsUnzipped();

        thenUncompressedFileExists();
        thenUncompressedFileIsAFile();
        thenUncompressedFileIsCorrect();
    }

    @Test
    public void unZipByteStreamTest() throws IOException {
        givenCompressedStream();

        whenStreamIsUnzipped();

        thenUncompressedFileExists();
        thenUncompressedFileIsAFile();
        thenUncompressedFileIsCorrect();
    }

    @Test
    public void catchExceptionWhenTooManyFilesTest() throws IOException {
        givenTooManyCompressedFile();

        whenTooManyFilesIsUnzipped();

        thenExceptionIsCaught();
    }

    @Test
    public void catchExceptionWhenTooBigFileTest() throws IOException {
        givenTooBigCompressedFile();

        whenTooBigFileIsUnzipped();

        thenExceptionIsCaught();
    }

    @Test
    public void catchExceptionWhenIllegalPathFileTest() throws IOException {
        givenIllegalPathCompressedFile();

        whenIllegalPathFileIsUnzipped();

        thenExceptionIsCaught();
    }

    @Test
    public void unZipMultipleFilesTest() throws IOException {
        givenMultipleCompressedFile();

        whenMultipleFileIsUnzipped();

        thenMultipleUncompressedFileExists();
    }

    @Test
    public void unZipUncompressedFileTest() throws IOException {
        givenUnCompressedFile();

        whenUncompressedFileIsUnzipped();

        thenUncompressedFileNotExist();
    }

    @Test
    public void unZipTarFileTest() throws IOException {
        givenTarCompressedFile();

        whenTarFileIsUnzipped();

        thenUncompressedFileNotExist();
    }

    private void givenCompressedFile() throws IOException {
        createOutputFolder();
        copyFile(SINGLE_INPUT_ZIP_FILE_PATH, SINGLE_INPUT_ZIP_FILE);
    }

    private void givenCompressedStream() throws IOException {
        createOutputFolder();
        this.compressedInput = getFileBytes(new File(SINGLE_INPUT_ZIP_FILE_PATH));
    }

    private void givenTooManyCompressedFile() throws IOException {
        createOutputFolder();
        copyFile(TOO_MANY_INPUT_ZIP_FILE_PATH, TOO_MANY_INPUT_ZIP_FILE);
    }

    private void givenTooBigCompressedFile() throws IOException {
        createOutputFolder();
        copyFile(TOO_BIG_INPUT_ZIP_FILE_PATH, TOO_BIG_INPUT_ZIP_FILE);
    }

    private void givenIllegalPathCompressedFile() throws IOException {
        createOutputFolder();
        copyFile(ILLEGAL_PATH_INPUT_ZIP_FILE_PATH, ILLEGAL_PATH_INPUT_ZIP_FILE);
    }

    private void givenMultipleCompressedFile() throws IOException {
        createOutputFolder();
        copyFile(MULTIPLE_INPUT_ZIP_FILE_PATH, MULTIPLE_INPUT_ZIP_FILE);
    }

    private void givenUnCompressedFile() throws IOException {
        createOutputFolder();
        copyFile(UNCOMPRESSED_INPUT_ZIP_FILE_PATH, UNCOMPRESSED_INPUT_ZIP_FILE);
    }

    private void givenTarCompressedFile() throws IOException {
        createOutputFolder();
        copyFile(INPUT_TAR_FILE_PATH, INPUT_TAR_FILE);
    }

    private void createOutputFolder() {
        File inputFolder = new File(WORK_FOLDER);
        inputFolder.mkdirs();
    }

    private void copyFile(String inputFilePath, String inputFileName) throws IOException {
        FileUtils.copyFile(new File(inputFilePath), new File(WORK_FOLDER + inputFileName));
    }

    private void whenFileIsUnzipped() throws IOException {
        UnZip.unZipFile(WORK_FOLDER + SINGLE_INPUT_ZIP_FILE, WORK_FOLDER);
    }

    private void whenStreamIsUnzipped() throws IOException {
        UnZip.unZipBytes(this.compressedInput, WORK_FOLDER);
    }

    private void whenTooManyFilesIsUnzipped() throws IOException {
        this.exceptionCaught = false;
        try {
            UnZip.unZipFile(WORK_FOLDER + TOO_MANY_INPUT_ZIP_FILE, WORK_FOLDER);
        } catch (IllegalStateException e) {
            this.exceptionCaught = true;
        }
    }

    private void whenTooBigFileIsUnzipped() throws IOException {
        this.exceptionCaught = false;
        try {
            UnZip.unZipFile(WORK_FOLDER + TOO_BIG_INPUT_ZIP_FILE, WORK_FOLDER);
        } catch (IllegalStateException e) {
            this.exceptionCaught = true;
        }
    }

    private void whenIllegalPathFileIsUnzipped() {
        this.exceptionCaught = false;
        try {
            UnZip.unZipFile(WORK_FOLDER + ILLEGAL_PATH_INPUT_ZIP_FILE, WORK_FOLDER);
        } catch (IOException e) {
            this.exceptionCaught = true;
        }
    }

    private void whenMultipleFileIsUnzipped() throws IOException {
        UnZip.unZipFile(WORK_FOLDER + MULTIPLE_INPUT_ZIP_FILE, WORK_FOLDER);
    }

    private void whenUncompressedFileIsUnzipped() throws IOException {
        UnZip.unZipFile(WORK_FOLDER + UNCOMPRESSED_INPUT_ZIP_FILE, WORK_FOLDER);
    }

    private void whenTarFileIsUnzipped() throws IOException {
        UnZip.unZipFile(WORK_FOLDER + INPUT_TAR_FILE, WORK_FOLDER);
    }

    private void thenUncompressedFileExists() {
        File uncompressedFile = new File(WORK_FOLDER + "file.txt");
        assertTrue(uncompressedFile.exists());
    }

    private void thenUncompressedFileIsAFile() {
        File uncompressedFile = new File(WORK_FOLDER + "file.txt");
        assertTrue(!uncompressedFile.isDirectory());
        assertTrue(uncompressedFile.isFile());
    }

    private void thenMultipleUncompressedFileExists() {
        File uncompressedFile = new File(WORK_FOLDER + "file.txt");
        File uncompressedFile1 = new File(WORK_FOLDER + "file1.txt");
        assertTrue(uncompressedFile.exists());
        assertTrue(uncompressedFile1.exists());
    }

    private void thenUncompressedFileIsCorrect() throws IOException {
        File file = new File(WORK_FOLDER + "file.txt");
        String content = FileUtils.readFileToString(file, "UTF-8");
        assertEquals("This is an awesome text file!\n", content);
    }

    private void thenExceptionIsCaught() {
        assertTrue(this.exceptionCaught);
    }

    private void thenUncompressedFileNotExist() {
        File uncompressedFile = new File(WORK_FOLDER + "file.txt");
        assertFalse(uncompressedFile.exists());
    }

    private static byte[] getFileBytes(File file) throws IOException {
        try (ByteArrayOutputStream ous = new ByteArrayOutputStream(); InputStream ios = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int read = 0;
            while ((read = ios.read(buffer)) != -1) {
                ous.write(buffer, 0, read);
            }
            return ous.toByteArray();
        }
    }
}
