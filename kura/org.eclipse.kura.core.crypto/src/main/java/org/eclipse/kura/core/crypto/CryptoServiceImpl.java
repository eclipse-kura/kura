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
 *     Red Hat Inc - Fix possible NPE, fix loading error
 *       - Clean up kura.properties handling
 *******************************************************************************/
package org.eclipse.kura.core.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private static final byte[] SECRET_KEY = "rv;ipse329183!@#".getBytes();

    private String m_keystorePasswordPath;

    private SystemService m_systemService;

    public void setSystemService(SystemService systemService) {
        this.m_systemService = systemService;
    }

    protected void activate() {
        if (this.m_systemService == null) {
            throw new IllegalStateException("Unable to get instance of: " + SystemService.class.getName());
        }

        this.m_keystorePasswordPath = this.m_systemService.getKuraDataDirectory() + File.separator + "store.save";
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
        Object convertedData = null;
        try {
            Class<?> clazz = Class.forName("javax.xml.bind.DatatypeConverter");
            Method method = clazz.getMethod("parseBase64Binary", String.class);
            convertedData = method.invoke(null, internalStringValue);
        } catch (ClassNotFoundException e) {
            convertedData = base64DecodeJava8(internalStringValue);
        } catch (LinkageError e) {
            convertedData = base64DecodeJava8(internalStringValue);
        } catch (Exception e) {

        }

        if (convertedData != null) {
            return (byte[]) convertedData;
        }
        return null;
    }

    private Object base64DecodeJava8(String internalStringValue) {
        Object convertedData = null;
        try {
            Class<?> clazz = Class.forName("java.util.Base64");
            Method decoderMethod = clazz.getMethod("getDecoder", (Class<?>[]) null);
            Object decoder = decoderMethod.invoke(null, new Object[0]);

            Class<?> base64Decoder = Class.forName("java.util.Base64$Decoder");
            Method decodeMethod = base64Decoder.getMethod("decode", String.class);
            convertedData = decodeMethod.invoke(decoder, internalStringValue);
        } catch (Exception e1) {
        }
        return convertedData;
    }

    private String base64Encode(byte[] encryptedBytes) {
        Object convertedData = null;
        try {
            Class<?> clazz = Class.forName("javax.xml.bind.DatatypeConverter");
            Method method = clazz.getMethod("printBase64Binary", byte[].class);
            convertedData = method.invoke(null, encryptedBytes);
        } catch (ClassNotFoundException e) {
            convertedData = base64EncodeJava8(encryptedBytes);
        } catch (LinkageError e) {
            convertedData = base64EncodeJava8(encryptedBytes);
        } catch (Exception e) {

        }

        if (convertedData != null) {
            return (String) convertedData;
        }
        return null;
    }

    private Object base64EncodeJava8(byte[] encryptedBytes) {
        Object convertedData = null;
        try {
            Class<?> clazz = Class.forName("java.util.Base64");
            Method encoderMethod = clazz.getMethod("getEncoder", (Class<?>[]) null);
            Object encoder = encoderMethod.invoke(null, new Object[0]);

            Class<?> base64Encoder = Class.forName("java.util.Base64$Encoder");
            Method encodeMethod = base64Encoder.getMethod("encodeToString", byte[].class);
            convertedData = encodeMethod.invoke(encoder, encryptedBytes);
        } catch (Exception e1) {
        }
        return convertedData;
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
    public String encodeBase64(String stringValue) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        byte[] bytesValue = stringValue.getBytes("UTF-8");
        String encodedValue = base64Encode(bytesValue);
        return encodedValue;

    }

    @Override
    public String decodeBase64(String encodedValue) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        byte[] decodedBytes = base64Decode(encodedValue);
        String decodedValue = new String(decodedBytes, "UTF-8");
        return decodedValue;
    }

    @Override
    public char[] getKeyStorePassword(String keyStorePath) {
        Properties props = new Properties();
        char[] password = null;
        FileInputStream fis = null;

        File f = new File(this.m_keystorePasswordPath);
        if (!f.exists()) {
            return "changeit".toCharArray();
        }

        try {
            fis = new FileInputStream(this.m_keystorePasswordPath);
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

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(this.m_keystorePasswordPath);
            props.store(fos, "Do not edit this file. It's automatically generated by Kura");
        } catch (FileNotFoundException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    logger.warn("IOException while closing destination - ", e);
                }
            }
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
        Key key = new SecretKeySpec(SECRET_KEY, ALGORITHM);
        return key;
    }
}
