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
    private static final String INPUT_ZIP_FILE = "compressedFile.zip";
    private static final String INPUT_ZIP_FILE_PATH = "target/test-classes/" + INPUT_ZIP_FILE;

    private byte[] compressedInput;

    @After
    public void clean() throws IOException {
        File compressedFile = new File(WORK_FOLDER + INPUT_ZIP_FILE);
        if (compressedFile.exists()) {
            FileUtils.delete(new File(WORK_FOLDER + INPUT_ZIP_FILE));
        }
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

    private void givenCompressedFile() throws IOException {
        File inputFolder = new File(WORK_FOLDER);
        inputFolder.mkdirs();
        FileUtils.copyFile(new File(INPUT_ZIP_FILE_PATH), new File(WORK_FOLDER + INPUT_ZIP_FILE));
    }

    private void givenCompressedStream() throws IOException {
        File inputFolder = new File(WORK_FOLDER);
        inputFolder.mkdirs();
        this.compressedInput = getFileBytes(new File(INPUT_ZIP_FILE_PATH));
    }

    private void whenFileIsUnzipped() throws IOException {
        UnZip.unZipFile(WORK_FOLDER + INPUT_ZIP_FILE, WORK_FOLDER);
    }

    private void whenStreamIsUnzipped() throws IOException {
        UnZip.unZipBytes(this.compressedInput, WORK_FOLDER);
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

    private void thenUncompressedFileIsCorrect() throws IOException {
        File file = new File(WORK_FOLDER + "file.txt");
        String content = FileUtils.readFileToString(file, "UTF-8");
        assertEquals("This is an awesome text file!\n", content);
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
