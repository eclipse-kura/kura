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
package org.eclipse.kura.web.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.kura.ssl.SslManagerService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.service.GwtCertificatesService;

public class GwtCertificatesServiceImpl extends OsgiRemoteServiceServlet implements GwtCertificatesService
{


	/**
	 * 
	 */
	private static final long serialVersionUID = 7402961266449489433L;


	public Integer storePublicPrivateKeys(String privateKey, String publicKey, String password, String alias)
			throws GwtKuraException {
		try {
			// Remove header if exists
			String key = privateKey.replace("-----BEGIN PRIVATE KEY-----", "").replace("\n", "");
			key = key.replace("-----END PRIVATE KEY-----", "");

			Object convertedData= null;
			try {
				Class<?> clazz = Class.forName("javax.xml.bind.DatatypeConverter");
				Method method = clazz.getMethod("parseBase64Binary", String.class);
				convertedData= method.invoke(null, key);
			} catch(ClassNotFoundException e) {
				convertedData = base64DecodeJava8(key);
			} catch (LinkageError e){
				convertedData = base64DecodeJava8(key);
			} catch (Exception e) {
				throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e);
			} 

			byte[] conversion= (byte[]) convertedData;
			// Parse Base64 - after PKCS8
			PKCS8EncodedKeySpec specPriv = new PKCS8EncodedKeySpec(conversion);

			// Create RSA key
			KeyFactory kf=KeyFactory.getInstance("RSA");        
			PrivateKey privKey = kf.generatePrivate(specPriv);

			Certificate[] certs= parsePublicCertificates(publicKey);

			if(privKey == null){
				throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
			}else{
				SslManagerService sslService = ServiceLocator.getInstance().getService(SslManagerService.class);
				sslService.installPrivateKey(alias, privKey, password.toCharArray(), certs);
			}
			return 1;
		} catch (UnsupportedEncodingException e) {
			throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
		} catch (GeneralSecurityException e) {
			throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
		} catch (IOException e) {
			throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
		}
	}


	public Integer storeLeafKey(String publicKey, String alias)
			throws GwtKuraException {
		try {
			Certificate[] certs= parsePublicCertificates(publicKey);

			if(certs.length == 0){
				throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
			}else{
				//Store only the first leaf passed. Don't consider other eventual certificates
				SslManagerService sslService = ServiceLocator.getInstance().getService(SslManagerService.class);
				X509Certificate sslCert= (X509Certificate) certs[0];

				sslService.installTrustCertificate("ssl-" + alias, sslCert);
			}
			return 1;
		} catch (CertificateException e) {
			throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
		} catch (UnsupportedEncodingException e) {
			throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
		} catch (GeneralSecurityException e) {
			throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
		} catch (IOException e) {
			throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
		}
	}

	public Integer storePublicChain(String publicKeys, String alias) throws GwtKuraException {
		try {
			Certificate[] certs= parsePublicCertificates(publicKeys);

			if(certs.length == 0){
				throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
			}else{
				int i= 0;
				SslManagerService sslService = ServiceLocator.getInstance().getService(SslManagerService.class);
				X509Certificate sslCert= (X509Certificate) certs[i];

				sslService.installTrustCertificate("ssl-" + alias, sslCert);
				i++;

				while(i < certs.length){
					X509Certificate caCert= (X509Certificate) certs[i];
					String certificateAlias= "ca-"+caCert.getSerialNumber().toString();

					sslService.installTrustCertificate(certificateAlias, caCert);
					i++;
				}
			}
			return certs.length;
		} catch (CertificateException e) {
			throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
		} catch (UnsupportedEncodingException e) {
			throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
		} catch (GeneralSecurityException e) {
			throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
		} catch (IOException e) {
			throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
		}
	}

	public Integer storeCertificationAuthority(String publicCAKeys, String alias)
			throws GwtKuraException {
		try {
			Certificate[] certs= parsePublicCertificates(publicCAKeys);

			if(certs.length == 0){
				throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
			}else{
				int i= 0;
				SslManagerService sslService = ServiceLocator.getInstance().getService(SslManagerService.class);
				X509Certificate sslCert= (X509Certificate) certs[i];

				sslService.installTrustCertificate("ca-"+alias, sslCert);
				i++;

				while(i < certs.length){
					X509Certificate caCert= (X509Certificate) certs[i];
					String certificateAlias= "ca-"+caCert.getSerialNumber().toString();

					sslService.installTrustCertificate(certificateAlias, caCert);
					i++;
				}
			}
			return certs.length;
		} catch (CertificateException e) {
			throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
		} catch (UnsupportedEncodingException e) {
			throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
		} catch (GeneralSecurityException e) {
			throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
		} catch (IOException e) {
			throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT, e);
		}
	}

	private Certificate[] parsePublicCertificates(String publicKey) throws CertificateException, UnsupportedEncodingException{
		CertificateFactory certFactory= CertificateFactory.getInstance("X.509");
		Collection<? extends Certificate> publicCertificates= certFactory.generateCertificates(new ByteArrayInputStream(publicKey.getBytes("UTF-8")));
		Iterator<? extends Certificate> certIterator= publicCertificates.iterator();

		Certificate[] certs= new Certificate[publicCertificates.size()];
		int i=0;

		while(certIterator.hasNext()){
			X509Certificate cert= (X509Certificate) certIterator.next();
			certs[i]= cert;
			i++;
		}
		return certs;
	}
	
	private Object base64DecodeJava8(String key) throws GwtKuraException{
		Object convertedData= null;
		try {
			Class<?> clazz = Class.forName("java.util.Base64");
			Method decoderMethod= clazz.getMethod("getDecoder", (Class<?>[]) null);
			Object decoder= decoderMethod.invoke(null, new Object[0]);

			Class<?> Base64Decoder = Class.forName("java.util.Base64$Decoder");
			Method decodeMethod = Base64Decoder.getMethod("decode", String.class);
			convertedData= decodeMethod.invoke(decoder, key);
		} catch (Exception e1) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, e1);
		}
		return convertedData;
	}
}