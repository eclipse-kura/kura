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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
                                               
	@Override
	public String encryptAes(String value) 
		throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException 
	{
		Key  key = generateKey();
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = c.doFinal(value.getBytes());
        
        String encryptedValue;
		try {
			encryptedValue = encodeBase64(new String(encryptedBytes));
		} catch (UnsupportedEncodingException e) {
			throw new NoSuchAlgorithmException(e);
		}
        return encryptedValue;	
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


	private static Key generateKey() 
	{
        Key key = new SecretKeySpec(SECRET_KEY, ALGORITHM);
        return key;
	}

	@Override
	public String sha1Hash(String s) 
		throws NoSuchAlgorithmException, UnsupportedEncodingException 
	{
		MessageDigest cript = MessageDigest.getInstance("SHA-1");
        cript.reset();
        cript.update(s.getBytes("UTF8"));
        
        byte[] encodedBytes = cript.digest();
        return encodeBase64(new String(encodedBytes));
	}

	@Override
	public String encodeBase64(String stringValue) 
		throws NoSuchAlgorithmException, UnsupportedEncodingException 
	{
		byte[] bytesValue = stringValue.getBytes("UTF-8");
		Object convertedData= null;
        try {
        	Class<?> clazz = Class.forName( "javax.xml.bind.DatatypeConverter" );
        	Method method = clazz.getMethod("printBase64Binary", byte.class);
        	convertedData= method.invoke(null, bytesValue);
        } catch( ClassNotFoundException e ) {
			try {
				Class<?> clazz = Class.forName("java.util.Base64");
	        	Method decoderMethod= clazz.getMethod("getEncoder", (Class<?>[]) null);
				Object encoder= decoderMethod.invoke(null, new Object[0]);
				
	        	Class<?> Base64Encoder = Class.forName("java.util.Base64$Encoder");
	        	Method decodeMethod = Base64Encoder.getMethod("encode", String.class);
	        	convertedData= decodeMethod.invoke(encoder, bytesValue);
			} catch (Exception e1) {
				throw new NoSuchAlgorithmException(e1);
			} 
        } catch (Exception e) {
        	throw new NoSuchAlgorithmException(e);
		} 
		String encodedValue = (String) convertedData; //DatatypeConverter.printBase64Binary(bytesValue);
        return encodedValue;	
		
	}

	@Override
	public String decodeBase64(String encodedValue) 
		throws NoSuchAlgorithmException, UnsupportedEncodingException 
	{
		Object convertedData= null;
        try {
        	Class<?> clazz = Class.forName( "javax.xml.bind.DatatypeConverter" );
        	Method method = clazz.getMethod("parseBase64Binary", String.class);
        	convertedData= method.invoke(null, encodedValue);
        } catch( ClassNotFoundException e ) {
			try {
				Class<?> clazz = Class.forName("java.util.Base64");
	        	Method decoderMethod= clazz.getMethod("getDecoder", (Class<?>[]) null);
				Object decoder= decoderMethod.invoke(null, new Object[0]);
				
	        	Class<?> Base64Decoder = Class.forName("java.util.Base64$Decoder");
	        	Method decodeMethod = Base64Decoder.getMethod("decode", String.class);
	        	convertedData= decodeMethod.invoke(decoder, encodedValue);
			} catch (Exception e1) {
				throw new NoSuchAlgorithmException(e1);
			} 
        } catch (Exception e) {
        	throw new NoSuchAlgorithmException(e);
		} 
        byte[] decodedBytes = (byte[]) convertedData;
        String decodedValue = new String(decodedBytes, "UTF-8");
        return decodedValue;		
	}

	@Override
	public char[] decryptAes(char[] encryptedValue) throws KuraException {
		Key  key = generateKey();
        Cipher c;
		try {
			c = Cipher.getInstance(ALGORITHM);
	        c.init(Cipher.DECRYPT_MODE, key);
	        String internalStringValue = new String(encryptedValue);
	        byte[] decordedValue  =  decodeBase64(internalStringValue).getBytes("UTF-8");
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
		} catch (UnsupportedEncodingException e) {
			throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED);
		}
	}
	
	@Override
	public char[] getKeyStorePassword(String keyStorePath) {
		return "changeit".toCharArray();
	}

	@Override
	public void setKeyStorePassword(String keyStorePath, String password)
			throws IOException {
		return;
	}
}
