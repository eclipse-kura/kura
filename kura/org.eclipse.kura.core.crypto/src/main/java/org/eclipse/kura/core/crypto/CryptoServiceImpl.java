/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.core.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
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
import javax.xml.bind.DatatypeConverter;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.crypto.CryptoService;

public class CryptoServiceImpl implements CryptoService {
	private static final String ALGORITHM   = "AES";
    private static final byte[] SECRET_KEY  = "rv;ipse329183!@#".getBytes();
    
    private static String s_keystorePasswordPath;
    
    static {
    	initKeystorePasswordPath();
    }
    
	@Override
	public char[] encryptAes(char[] value) throws KuraException {        
        String encryptedValue = null;
        
		try {
			Key  key = generateKey();
			Cipher c = Cipher.getInstance(ALGORITHM);
	        c.init(Cipher.ENCRYPT_MODE, key);
	        byte[] encryptedBytes = c.doFinal(new String(value).getBytes());
	        encryptedValue = DatatypeConverter.printBase64Binary(encryptedBytes);
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

	@Override
	public char[] decryptAes(char[] encryptedValue) throws KuraException {
		Key  key = generateKey();
        Cipher c;
		try {
			c = Cipher.getInstance(ALGORITHM);
	        c.init(Cipher.DECRYPT_MODE, key);
	        String internalStringValue = new String(encryptedValue);
	        byte[] decordedValue  =  DatatypeConverter.parseBase64Binary(internalStringValue);
	        byte[] decryptedBytes = c.doFinal(decordedValue);
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
	public String encryptAes(String value) 
		throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException 
	{
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
	public String decryptAes(String encryptedValue) 
		throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException 
	{
		try {
			return new String(decryptAes(encryptedValue.toCharArray()));
		} catch (KuraException e) {
			throw new IOException();
		}
    }

	@Override
	public String sha1Hash(String s) 
		throws NoSuchAlgorithmException, UnsupportedEncodingException 
	{
		MessageDigest cript = MessageDigest.getInstance("SHA-1");
        cript.reset();
        cript.update(s.getBytes("UTF8"));
        
        byte[] encodedBytes = cript.digest();
        return DatatypeConverter.printBase64Binary(encodedBytes);
	}

	@Override
	public String encodeBase64(String stringValue) 
		throws NoSuchAlgorithmException, UnsupportedEncodingException 
	{
		byte[] bytesValue = stringValue.getBytes("UTF-8");
		String encodedValue = DatatypeConverter.printBase64Binary(bytesValue);
        return encodedValue;	
		
	}

	@Override
	public String decodeBase64(String encodedValue) 
		throws NoSuchAlgorithmException, UnsupportedEncodingException 
	{
        byte[] decodedBytes = DatatypeConverter.parseBase64Binary(encodedValue);
        String decodedValue = new String(decodedBytes, "UTF-8");
        return decodedValue;		
	}
		
	@Override
	public char[] getKeyStorePassword(String keyStorePath) {
		Properties props = new Properties();
		char[] password = null;
		FileInputStream fis = null;
		
		File f = new File(s_keystorePasswordPath);
		if (!f.exists()) {
			return "changeit".toCharArray();
		}
		
		try {
			fis = new FileInputStream(s_keystorePasswordPath);
			props.load(fis);
			Object value = props.get(keyStorePath);
			if (value != null) {
				String encryptedPassword = (String) value;
				password = decryptAes(encryptedPassword.toCharArray());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (KuraException e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
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
			fos = new FileOutputStream(s_keystorePasswordPath);
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
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	@Deprecated
	public void setKeyStorePassword(String keyStorePath, String password)
			throws IOException {
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
	
	private static Key generateKey() 
	{
        Key key = new SecretKeySpec(SECRET_KEY, ALGORITHM);
        return key;
	}
	
	private static void initKeystorePasswordPath() {
    	String uriSpec = System.getProperty("kura.configuration");
    	Properties props = new Properties();
		FileInputStream fis = null;
		try {
		   	URI uri = new URI(uriSpec);
			fis = new FileInputStream(new File(uri));
			props.load(fis);
			Object value = props.get("kura.data");
			if (value != null) {
				s_keystorePasswordPath = (String) value + File.separator + "store.save"; 
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
