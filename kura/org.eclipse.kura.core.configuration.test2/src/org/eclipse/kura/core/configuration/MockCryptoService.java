/*******************************************************************************
 * Copyright (c) 2016, 2017 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.core.configuration;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.crypto.CryptoService;

public class MockCryptoService implements CryptoService {

    @Override
    public char[] encryptAes(char[] value) throws KuraException {
        return value;
    }

    @Override
    public char[] decryptAes(char[] encryptedValue) throws KuraException {
        return encryptedValue;
    }

    @Override
    public String encryptAes(String value) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        // TODO Auto-generated method stub
        return value;
    }

    @Override
    public String decryptAes(String encryptedValue) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException {
        // TODO Auto-generated method stub
        return encryptedValue;
    }

    @Override
    public String sha1Hash(String s) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String encodeBase64(String stringValue) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String decodeBase64(String encodedValue) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public char[] getKeyStorePassword(String keyStorePath) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setKeyStorePassword(String keyStorePath, char[] password) throws KuraException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setKeyStorePassword(String keyStorePath, String password) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isFrameworkSecure() {
        // TODO Auto-generated method stub
        return false;
    }

}
