/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.core.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.system.SystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CryptoServiceImpl implements CryptoService {

    private static final Logger logger = LoggerFactory.getLogger(CryptoServiceImpl.class);

    private static final String ALGORITHM = "AES";
    private static final byte[] SECRET_KEY = System
            .getProperty("org.eclipse.kura.core.crypto.secretKey", "rv;ipse329183!@#").getBytes();

    private String keystorePasswordPath;

    private SystemService systemService;

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void unsetSystemService(SystemService systemService) {
        this.systemService = null;
    }

    protected void activate() {
        if (this.systemService == null) {
            throw new IllegalStateException("Unable to get instance of: " + SystemService.class.getName());
        }

        this.keystorePasswordPath = this.systemService.getKuraDataDirectory() + File.separator + "store.save";
    }

    @Override
    public char[] encryptAes(char[] value) throws KuraException {
        String encryptedValue = null;

        try {
            Key key = generateKey();
            Cipher c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = c.doFinal(new String(value).getBytes());
            encryptedValue = base64Encode(encryptedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
        } catch (NoSuchPaddingException e) {
            throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
        } catch (InvalidKeyException e) {
            throw new KuraException(KuraErrorCode.ENCODE_ERROR);
        } catch (IllegalBlockSizeException e) {
            throw new KuraException(KuraErrorCode.ENCODE_ERROR);
        } catch (BadPaddingException e) {
            throw new KuraException(KuraErrorCode.ENCODE_ERROR);
        }

        return encryptedValue.toCharArray();
    }

    private byte[] base64Decode(String internalStringValue) {
        return Base64.getDecoder().decode(internalStringValue);
    }

    private String base64Encode(byte[] encryptedBytes) {
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    @Override
    public char[] decryptAes(char[] encryptedValue) throws KuraException {
        Key key = generateKey();
        Cipher c;
        try {
            c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, key);
            String internalStringValue = new String(encryptedValue);
            byte[] decodedValue = base64Decode(internalStringValue);
            if (encryptedValue.length > 0 && decodedValue.length == 0) {
                throw new KuraException(KuraErrorCode.DECODER_ERROR);
            }
            byte[] decryptedBytes = c.doFinal(decodedValue);
            String decryptedValue = new String(decryptedBytes);
            return decryptedValue.toCharArray();
        } catch (NoSuchAlgorithmException e) {
            throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
        } catch (NoSuchPaddingException e) {
            throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
        } catch (InvalidKeyException e) {
            throw new KuraException(KuraErrorCode.DECODER_ERROR);
        } catch (BadPaddingException e) {
            throw new KuraException(KuraErrorCode.DECODER_ERROR);
        } catch (IllegalBlockSizeException e) {
            throw new KuraException(KuraErrorCode.DECODER_ERROR);
        }
    }

    @Override
    @Deprecated
    public String encryptAes(String value) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        char[] encryptedValue = null;
        try {
            encryptedValue = encryptAes(value.toCharArray());
        } catch (KuraException e) {
            Throwable t = e.getCause();
            if (t instanceof NoSuchAlgorithmException) {
                throw (NoSuchAlgorithmException) t;
            } else if (t instanceof NoSuchPaddingException) {
                throw (NoSuchPaddingException) t;
            } else if (t instanceof InvalidKeyException) {
                throw (InvalidKeyException) t;
            } else if (t instanceof IllegalBlockSizeException) {
                throw (IllegalBlockSizeException) t;
            } else if (t instanceof BadPaddingException) {
                throw (BadPaddingException) t;
            }
        }

        return new String(encryptedValue);
    }

    @Override
    @Deprecated
    public String decryptAes(String encryptedValue) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException {
        try {
            return new String(decryptAes(encryptedValue.toCharArray()));
        } catch (KuraException e) {
            throw new IOException();
        }
    }

    @Override
    public String sha1Hash(String s) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest cript = MessageDigest.getInstance("SHA-1");
        cript.reset();
        cript.update(s.getBytes("UTF8"));

        byte[] encodedBytes = cript.digest();
        return base64Encode(encodedBytes);
    }

    @Override
    public String encodeBase64(String stringValue) throws UnsupportedEncodingException {
        if (stringValue == null)
            return null;

        return base64Encode(stringValue.getBytes("UTF-8"));
    }

    @Override
    public String decodeBase64(String encodedValue) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if (encodedValue == null)
            return null;

        return new String(base64Decode(encodedValue), "UTF-8");
    }

    @Override
    public char[] getKeyStorePassword(String keyStorePath) {
        Properties props = new Properties();
        char[] password = null;
        FileInputStream fis = null;

        File f = new File(this.keystorePasswordPath);
        if (!f.exists()) {
            return "changeit".toCharArray();
        }

        try {
            fis = new FileInputStream(this.keystorePasswordPath);
            props.load(fis);
            Object value = props.get(keyStorePath);
            if (value != null) {
                String encryptedPassword = (String) value;
                password = decryptAes(encryptedPassword.toCharArray());
            }
        } catch (FileNotFoundException e) {
            logger.warn("File not found exception while getting keystore password - ", e);
        } catch (IOException e) {
            logger.warn("IOException while getting keystore password - ", e);
        } catch (KuraException e) {
            logger.warn("KuraException while getting keystore password - ", e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    logger.warn("IOException while closing source - ", e);
                }
            }
        }

        return password;
    }

    @Override
    public void setKeyStorePassword(String keyStorePath, char[] password) throws KuraException {
        Properties props = new Properties();
        char[] encryptedPassword = encryptAes(password);
        props.put(keyStorePath, new String(encryptedPassword));

        try (FileOutputStream fos = new FileOutputStream(this.keystorePasswordPath);){
            props.store(fos, "Do not edit this file. It's automatically generated by Kura");
            fos.flush();
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    @Deprecated
    public void setKeyStorePassword(String keyStorePath, String password) throws IOException {
        try {
            setKeyStorePassword(keyStorePath, password.toCharArray());
        } catch (KuraException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean isFrameworkSecure() {
        return false;
    }

    private static Key generateKey() {
        return new SecretKeySpec(SECRET_KEY, ALGORITHM);
    }
}
