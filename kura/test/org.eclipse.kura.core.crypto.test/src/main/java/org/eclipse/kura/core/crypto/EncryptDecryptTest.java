package org.eclipse.kura.core.crypto;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.kura.KuraException;
import org.junit.Before;
import org.junit.Test;

public class EncryptDecryptTest {

    private CryptoServiceImpl cryptoService;

    @Before
    public void setup() {
        this.cryptoService = new CryptoServiceImpl();
    }

    @Test
    public void shouldStreamEncodeAndStreamDecodeCorrectly() throws KuraException, IOException {

        String dataToEncrypt = "testingcrypto";
        File tempFile = new File("/tmp/testingCrypto.txt");
        tempFile.deleteOnExit();

        try (FileOutputStream out = new FileOutputStream(tempFile);
                OutputStream cryptoOut = this.cryptoService.getEncryptionOutputStream(out);) {

            cryptoOut.write(dataToEncrypt.getBytes());
            cryptoOut.flush();
            out.flush();
            out.getFD().sync();
        }

        try (FileInputStream input = new FileInputStream(new File("/tmp/testingCrypto.txt"));
                InputStream cryptoInput = this.cryptoService.getDecryptionInputStream(input);) {
            String dataDecrypted = IOUtils.toString(cryptoInput, StandardCharsets.UTF_8);

            assertEquals(dataToEncrypt, dataDecrypted);
        }

        tempFile.delete();

    }

    @Test
    public void shouldReturnSameDecodingOutputWithStreamAndChars() throws KuraException, IOException {

        String dataToEncrypt = "testingcrypto";
        File tempFile = new File("/tmp/testingCrypto.txt");
        tempFile.deleteOnExit();

        try (FileOutputStream out = new FileOutputStream(tempFile);
                OutputStream cryptoOut = this.cryptoService.getEncryptionOutputStream(out);) {

            cryptoOut.write(dataToEncrypt.getBytes());
            cryptoOut.flush();
            out.flush();
            out.getFD().sync();
        }

        char[] charEncryptedData = this.cryptoService.encryptAes(dataToEncrypt.toCharArray());

        try (FileInputStream input = new FileInputStream(new File("/tmp/testingCrypto.txt"));
                InputStream cryptoInput = this.cryptoService.getDecryptionInputStream(input);) {

            assertEquals(new String(this.cryptoService.decryptAes(charEncryptedData)),
                    IOUtils.toString(cryptoInput, StandardCharsets.UTF_8));
        }

        tempFile.delete();

    }

    @Test
    public void shouldStramEncryptAndCharDecryptCorrectly() throws KuraException, IOException {
        String dataToEncrypt = "testingcrypto";
        File tempFile = new File("/tmp/testingCrypto.txt");
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile);
                OutputStream cryptoOut = this.cryptoService.getEncryptionOutputStream(out);) {

            cryptoOut.write(dataToEncrypt.getBytes());
            cryptoOut.flush();
            out.flush();
            out.getFD().sync();
        }

        try (FileInputStream input = new FileInputStream(new File("/tmp/testingCrypto.txt"))) {
            String cryptedData = IOUtils.toString(input, StandardCharsets.UTF_8);
            String decryptedData = new String(this.cryptoService.decryptAes(cryptedData.toCharArray()));

            assertEquals(dataToEncrypt, decryptedData);
        }

        tempFile.delete();
    }

    @Test
    public void shouldCharEncryptAndStreamDecryptCorrectly() throws KuraException, IOException {
        String dataToEncrypt = "testingcrypto";
        File tempFile = new File("/tmp/testingCrypto.txt");
        tempFile.deleteOnExit();

        char[] cryptedData = this.cryptoService.encryptAes(dataToEncrypt.toCharArray());

        try (ByteArrayInputStream input = new ByteArrayInputStream(new String(cryptedData).getBytes());
                InputStream cryptoInput = this.cryptoService.getDecryptionInputStream(input);) {

            String decryptedData = IOUtils.toString(cryptoInput, StandardCharsets.UTF_8);
            assertEquals(dataToEncrypt, decryptedData);
        }
    }
}
