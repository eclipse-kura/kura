/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.crypto;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (OutputStream cryptoOut = this.cryptoService.aesEncryptingStream(out);) {

            cryptoOut.write(dataToEncrypt.getBytes(StandardCharsets.UTF_8));
            cryptoOut.flush();
            out.flush();
        }

        try (ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());
                InputStream cryptoInput = this.cryptoService.aesDecryptingStream(input);) {
            String dataDecrypted = IOUtils.toString(cryptoInput, StandardCharsets.UTF_8);

            assertEquals(dataToEncrypt, dataDecrypted);
        }

    }

    @Test
    public void shouldReturnSameDecodingOutputWithStreamAndChars() throws KuraException, IOException {

        String dataToEncrypt = "testingcrypto";
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (OutputStream cryptoOut = this.cryptoService.aesEncryptingStream(out);) {

            cryptoOut.write(dataToEncrypt.getBytes(StandardCharsets.UTF_8));
            cryptoOut.flush();
            out.flush();
        }

        char[] charEncryptedData = this.cryptoService.encryptAes(dataToEncrypt.toCharArray());

        try (ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());
                InputStream cryptoInput = this.cryptoService.aesDecryptingStream(input);) {

            assertEquals(new String(this.cryptoService.decryptAes(charEncryptedData)),
                    IOUtils.toString(cryptoInput, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void shouldStramEncryptAndCharDecryptCorrectly() throws KuraException, IOException {
        String dataToEncrypt = "testingcrypto";
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (OutputStream cryptoOut = this.cryptoService.aesEncryptingStream(out);) {

            cryptoOut.write(dataToEncrypt.getBytes(StandardCharsets.UTF_8));
            cryptoOut.flush();
            out.flush();
        }

        try (ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray())) {
            String cryptedData = IOUtils.toString(input, StandardCharsets.UTF_8);
            String decryptedData = new String(this.cryptoService.decryptAes(cryptedData.toCharArray()));

            assertEquals(dataToEncrypt, decryptedData);
        }
    }

    @Test
    public void shouldCharEncryptAndStreamDecryptCorrectly() throws KuraException, IOException {
        String dataToEncrypt = "testingcrypto";

        char[] cryptedData = this.cryptoService.encryptAes(dataToEncrypt.toCharArray());

        try (ByteArrayInputStream input = new ByteArrayInputStream(
                new String(cryptedData).getBytes(StandardCharsets.UTF_8));
                InputStream cryptoInput = this.cryptoService.aesDecryptingStream(input);) {

            String decryptedData = IOUtils.toString(cryptoInput, StandardCharsets.UTF_8);
            assertEquals(dataToEncrypt, decryptedData);
        }
    }
}
