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
    private static final String SINGLE_INPUT_ZIP_FILE_PATH = "target/test-classes/" + SINGLE_INPUT_ZIP_FILE;
    private static final String MULTIPLE_INPUT_ZIP_FILE_PATH = "target/test-classes/" + MULTIPLE_INPUT_ZIP_FILE;
    private static final String TOO_MANY_INPUT_ZIP_FILE_PATH = "target/test-classes/" + TOO_MANY_INPUT_ZIP_FILE;
    private static final String TOO_BIG_INPUT_ZIP_FILE_PATH = "target/test-classes/" + TOO_BIG_INPUT_ZIP_FILE;
    private static final String ILLEGAL_PATH_INPUT_ZIP_FILE_PATH = "target/test-classes/" + ILLEGAL_PATH_INPUT_ZIP_FILE;

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

    private void givenCompressedFile() throws IOException {
        File inputFolder = new File(WORK_FOLDER);
        inputFolder.mkdirs();
        FileUtils.copyFile(new File(SINGLE_INPUT_ZIP_FILE_PATH), new File(WORK_FOLDER + SINGLE_INPUT_ZIP_FILE));
    }

    private void givenCompressedStream() throws IOException {
        File inputFolder = new File(WORK_FOLDER);
        inputFolder.mkdirs();
        this.compressedInput = getFileBytes(new File(SINGLE_INPUT_ZIP_FILE_PATH));
    }

    private void givenTooManyCompressedFile() throws IOException {
        File inputFolder = new File(WORK_FOLDER);
        inputFolder.mkdirs();
        FileUtils.copyFile(new File(TOO_MANY_INPUT_ZIP_FILE_PATH), new File(WORK_FOLDER + TOO_MANY_INPUT_ZIP_FILE));
    }

    private void givenTooBigCompressedFile() throws IOException {
        File inputFolder = new File(WORK_FOLDER);
        inputFolder.mkdirs();
        FileUtils.copyFile(new File(TOO_BIG_INPUT_ZIP_FILE_PATH), new File(WORK_FOLDER + TOO_BIG_INPUT_ZIP_FILE));
    }

    private void givenIllegalPathCompressedFile() throws IOException {
        File inputFolder = new File(WORK_FOLDER);
        inputFolder.mkdirs();
        FileUtils.copyFile(new File(ILLEGAL_PATH_INPUT_ZIP_FILE_PATH),
                new File(WORK_FOLDER + ILLEGAL_PATH_INPUT_ZIP_FILE));
    }

    private void givenMultipleCompressedFile() throws IOException {
        File inputFolder = new File(WORK_FOLDER);
        inputFolder.mkdirs();
        FileUtils.copyFile(new File(MULTIPLE_INPUT_ZIP_FILE_PATH), new File(WORK_FOLDER + MULTIPLE_INPUT_ZIP_FILE));
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
