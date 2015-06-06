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

import javax.xml.bind.DatatypeConverter;

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


	public Integer storePrivateSSLCertificate(String privateCert, String publicCert, String password, String alias)
			throws GwtKuraException {
		try {
	    	// Remove header if exists
	        String key = privateCert.replace("-----BEGIN PRIVATE KEY-----", "").replace("\n", "");
	        key = key.replace("-----END PRIVATE KEY-----", "");
	    	
	        byte[] conversion= DatatypeConverter.parseBase64Binary(key);
	        // Parse Base64 - after PKCS8
	        PKCS8EncodedKeySpec specPriv = new PKCS8EncodedKeySpec(conversion);
		    
	        // Create RSA key
	        KeyFactory kf=KeyFactory.getInstance("RSA");        
	        PrivateKey privKey = kf.generatePrivate(specPriv);
			
			CertificateFactory certFactory= CertificateFactory.getInstance("X.509");
			Collection<? extends Certificate> publicCertificates= certFactory.generateCertificates(new ByteArrayInputStream(publicCert.getBytes("UTF-8")));
			
			Certificate[] certs= parsePublicCertificates(publicCertificates);

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


	public Integer storePublicSSLCertificate(String publicCert, String alias)
			throws GwtKuraException {
		try {
			CertificateFactory certFactory= CertificateFactory.getInstance("X.509");
			Collection<? extends Certificate> publicCertificates= certFactory.generateCertificates(new ByteArrayInputStream(publicCert.getBytes("UTF-8")));
			
			Certificate[] certs= parsePublicCertificates(publicCertificates);
			
			if(certs.length == 0){
				throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ARGUMENT);
			}else{
				int i= 0;
				SslManagerService sslService = ServiceLocator.getInstance().getService(SslManagerService.class);
				X509Certificate sslCert= (X509Certificate) certs[i];
				
				sslService.installTrustCertificate(alias, sslCert);
				i++;
				
				while(i < certs.length){
					X509Certificate caCert= (X509Certificate) certs[i];
					String certificateAlias= "ca-"+caCert.getSerialNumber().toString();
						
					sslService.installTrustCertificate(certificateAlias, caCert);
					i++;
				}
			}
			return publicCertificates.size();
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
	
	private Certificate[] parsePublicCertificates(Collection<? extends Certificate> publicCertificates){
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
}