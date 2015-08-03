package org.eclipse.kura.core.deployment.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;

public class HashUtil {
	private static String base64Encode(byte[] encryptedBytes){
		Object convertedData= null;
		try {
			Class<?> clazz = Class.forName( "javax.xml.bind.DatatypeConverter" );
			Method method = clazz.getMethod("printBase64Binary", byte[].class);
			convertedData= method.invoke(null, encryptedBytes);
		} catch( ClassNotFoundException e ) {
			try {
				Class<?> clazz = Class.forName("java.util.Base64");
				Method encoderMethod= clazz.getMethod("getEncoder", (Class<?>[]) null);
				Object encoder= encoderMethod.invoke(null, new Object[0]);

				Class<?> Base64Decoder = Class.forName("java.util.Base64$Encoder");
				Method decodeMethod = Base64Decoder.getMethod("encodeToString", byte[].class);
				convertedData= decodeMethod.invoke(encoder, encryptedBytes);
			} catch (Exception e1) {
			} 

		} catch (Exception e) {
		}

		if(convertedData != null){
			return (String) convertedData;
		}
		return null;
	}

	public static String hash(String digestAlgorithm, File file) throws IOException, KuraException
	{
		MessageDigest cript = null;
		FileInputStream fis = null;
		try {
			cript = MessageDigest.getInstance(digestAlgorithm);
			fis = new FileInputStream(file);

			byte[] byteArray = new byte[1024];
			int bytesCount = 0;
			while ((bytesCount = fis.read(byteArray)) != -1) {
				cript.update(byteArray, 0, bytesCount);
			}
			byte[] encodedBytes = cript.digest();
			return base64Encode(encodedBytes);
		} catch (FileNotFoundException e) {
			throw new KuraException(KuraErrorCode.STORE_ERROR, e);
		} catch (IOException e) {
			throw new KuraException(KuraErrorCode.STORE_ERROR, e);
		} catch (NoSuchAlgorithmException e) {
			throw new KuraException(KuraErrorCode.OPERATION_NOT_SUPPORTED, e.getMessage());
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
	}

}
