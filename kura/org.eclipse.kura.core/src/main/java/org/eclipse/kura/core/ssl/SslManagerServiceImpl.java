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
package org.eclipse.kura.core.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.LoadStoreParameter;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.certificate.CertificatesService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.ssl.SslManagerService;
import org.eclipse.kura.ssl.SslServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslManagerServiceImpl implements SslManagerService, ConfigurableComponent
{
	private static final Logger s_logger = LoggerFactory.getLogger(SslManagerServiceImpl.class);
	private static final String APP_PID = "service.pid";
	private static ComponentContext s_context;

	private SslServiceListeners		 m_sslServiceListeners;

	@SuppressWarnings("unused")
	private ComponentContext         m_ctx;
	private SslManagerServiceOptions m_options;

	private CertificatesService m_certificatesService;
	private CryptoService m_cryptoService;
	private ConfigurationService m_configurationService;
	private SecureRandom random = new SecureRandom();


	private Map<String, Object> m_properties;

	private ExecutorService m_worker;
	private Future<?> m_handle;

	private boolean serviceEnabled = true;



	public SslManagerServiceImpl() {
		super();
		m_worker = Executors.newSingleThreadExecutor();
	}

	// ----------------------------------------------------------------
	//
	//   Dependencies
	//
	// ----------------------------------------------------------------

	public void setCryptoService(CryptoService cryptoService) {
		this.m_cryptoService = cryptoService;
	}

	public void unsetCryptoService(CryptoService cryptoService) {
		this.m_cryptoService = null;
	}

	public void setConfigurationService(ConfigurationService configurationService) {
		this.m_configurationService = configurationService;
	}

	public void unsetConfigurationService(ConfigurationService configurationService) {
		this.m_configurationService = null;
	}

	// ----------------------------------------------------------------
	//
	//   Activation APIs
	//
	// ----------------------------------------------------------------

	protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
	{
		s_logger.info("activate...");

		s_context = componentContext;
		//
		// save the bundle context and the properties
		m_ctx = componentContext;
		m_options = new SslManagerServiceOptions(properties);

		ServiceTracker<SslServiceListener, SslServiceListener> listenersTracker = new ServiceTracker<SslServiceListener, SslServiceListener>(
				componentContext.getBundleContext(),
				SslServiceListener.class, null);

		// Deferred open of tracker to prevent
		// java.lang.Exception: Recursive invocation of ServiceFactory.getService
		// on ProSyst
		m_sslServiceListeners = new SslServiceListeners(listenersTracker);

		decryptProperties(properties);

		char[] keystorePassword= m_cryptoService.getKeyStorePassword(m_options.getSslTrustStore());

		if(m_options.getSslKeystorePassword() == null && keystorePassword != null && verifyEnvironmentProperties(keystorePassword)){

				String randomValue= new BigInteger(160, random).toString(32);


				HashMap<String, Object> propertiesCopy= new HashMap<String, Object>();
				Iterator<String> keys1 = properties.keySet().iterator();
				while (keys1.hasNext()) {
					String key = keys1.next();
					Object value = properties.get(key);
					propertiesCopy.put(key, value);
				}

				propertiesCopy.put(SslManagerServiceOptions.PROP_TRUST_PASSWORD, new Password(randomValue.toCharArray()));
				try {

					changeSSLKeystorePassword(keystorePassword, randomValue.toCharArray());
					doUpdate(false, propertiesCopy);
				} catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
					e.printStackTrace();
				} 
				
		}else if(m_options.getSslKeystorePassword() != null && !verifyEnvironmentProperties(m_options.getSslKeystorePassword().toCharArray())){
			serviceEnabled= false;
		}
	}

	public void updated(Map<String,Object> properties)
	{
		s_logger.info("updated...: " + properties);

		decryptProperties(properties);

		char[] oldPassword= m_cryptoService.getKeyStorePassword(m_options.getSslTrustStore());
		try {

			char[] newPassword= (char[]) m_properties.get(SslManagerServiceOptions.PROP_TRUST_PASSWORD);

			if(oldPassword != null && !Arrays.equals(oldPassword, newPassword)){
				changeSSLKeystorePassword(oldPassword, newPassword);
			}else if(oldPassword == null){
				changeSSLKeystorePassword(newPassword, newPassword);
			}
		} catch (Exception e) {
			s_logger.info("Problem managing SSL keystore");
		}

		// Update properties and re-publish Birth certificate
		m_options = new SslManagerServiceOptions(properties);
		// Notify listeners that service has been updated
		m_sslServiceListeners.onConfigurationUpdated();

	}

	protected void deactivate(ComponentContext componentContext) 
	{
		s_logger.info("deactivate...");
		m_sslServiceListeners.close();
	}


	// ----------------------------------------------------------------
	//
	//   Service APIs
	//
	// ----------------------------------------------------------------

	@Override
	public SSLSocketFactory getSSLSocketFactory() 
			throws GeneralSecurityException, IOException
	{

		String ciphers  = m_options.getSslCiphers();
		String protocol = m_options.getSslProtocol();

		String  trustStore = m_options.getSslTrustStore();
		TrustManager[] tms = getTrustManagers(trustStore);

		if(tms == null){
			throw new GeneralSecurityException("SSL keystore tampered!");
		}

		String keyAlias    = null;
		char[] keyStorePwd = getKeyStorePassword();         
		KeyManager[]   kms = getKeyManagers(trustStore, keyStorePwd, keyAlias);

		return getSSLSocketFactory(protocol, ciphers, kms, tms);

	}


	@Override
	public SSLSocketFactory getSSLSocketFactory(String keyAlias)
			throws GeneralSecurityException, IOException
	{
		String protocol = m_options.getSslProtocol();
		String ciphers  = m_options.getSslCiphers();

		String  trustStore = m_options.getSslTrustStore();
		TrustManager[] tms = getTrustManagers(trustStore);

		if(tms == null){
			throw new GeneralSecurityException("SSL keystore tampered!");
		}

		char[] keyStorePwd = getKeyStorePassword();         
		KeyManager[]   kms = getKeyManagers(trustStore, keyStorePwd, keyAlias);

		return getSSLSocketFactory(protocol, ciphers, kms, tms);

	}

	@Override
	public SSLSocketFactory getSSLSocketFactory(String protocol,
			String ciphers,
			String trustStore,
			String keyStore,
			char[] keyStorePwd,
			String keyAlias)
					throws GeneralSecurityException, IOException
	{

		TrustManager[] tms = getTrustManagers(trustStore);

		if(tms == null){
			throw new GeneralSecurityException("SSL keystore tampered!");
		}

		KeyManager[]   kms = getKeyManagers(keyStore, keyStorePwd, keyAlias);
		return getSSLSocketFactory(protocol, ciphers, kms, tms);

	}

	@Override
	public X509Certificate[] getTrustCertificates() 
			throws GeneralSecurityException, IOException
	{
		// trust store
		X509Certificate[] cacerts = null;
		String  trustStore = m_options.getSslTrustStore();
		TrustManager[] tms = getTrustManagers(trustStore);
		for (TrustManager tm : tms) {            
			if (tm instanceof X509TrustManager) {
				X509TrustManager x509tm = (X509TrustManager) tm;
				cacerts = x509tm.getAcceptedIssuers();
				break;
				//                for (X509Certificate x509cert : x509certs) {
				//                    System.err.println("TS DN:        "+x509cert.getSubjectDN());
				//                    System.err.println("TS DN:        "+x509cert.getSubjectX500Principal().getName());
				//                    System.err.println("TS CANONICAL: "+x509cert.getSubjectX500Principal().getName(X500Principal.CANONICAL));
				//                    System.err.println("TS RFC1779:   "+x509cert.getSubjectX500Principal().getName(X500Principal.RFC1779));
				//                    System.err.println("TS RFC2253:   "+x509cert.getSubjectX500Principal().getName(X500Principal.RFC2253));
				//                    System.err.println("TS alt:       "+x509cert.getSubjectAlternativeNames());
				//                    System.err.println("TS not before date: "+x509cert.getNotBefore());
				//                    System.err.println("TS not after  date: "+x509cert.getNotAfter());
				//                }
			}
		}
		return cacerts;
	}


	@Override
	public void installTrustCertificate(String alias, X509Certificate x509crt) 
			throws GeneralSecurityException, IOException
	{
		InputStream tsReadStream = null;
		FileOutputStream tsOutStream = null;

		try{
			// load the trust store
			String trustStore = m_options.getSslTrustStore();
			KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
			File fTrustStore = new File(trustStore);
			if (fTrustStore.exists()) {
				tsReadStream = new FileInputStream(trustStore);
				ts.load(tsReadStream, null);
			}
			else {
				ts.load(null, null);
			}

			// add the certificate
			ts.setCertificateEntry(alias, x509crt);

			// save it
			char[] trustStorePwd = getKeyStorePassword(); 
			tsOutStream = new FileOutputStream(trustStore);
			ts.store(tsOutStream, trustStorePwd);
			tsOutStream.close();
		}
		finally{
			if(tsReadStream != null) tsReadStream.close();
			if(tsOutStream != null) tsOutStream.close();
		}
	}


	@Override
	public void deleteTrustCertificate(String alias)
			throws GeneralSecurityException, IOException
	{
		InputStream tsReadStream = null;

		try{
			// load the trust store
			String trustStore = m_options.getSslTrustStore();
			KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
			tsReadStream = new FileInputStream(trustStore);
			ts.load(tsReadStream, null);

			// delete the entry
			ts.deleteEntry(alias);

			// save it
			ts.store( new LoadStoreParameter() {            
				@Override
				public ProtectionParameter getProtectionParameter() {
					char[] trustStorePwd;
					try {
						trustStorePwd = getKeyStorePassword();
						return new PasswordProtection(trustStorePwd);
					}
					catch (Exception e) {
						s_logger.error("Error loading TrustStore password", e);
					} 
					return null;
				}
			}); 
		}
		finally{
			if(tsReadStream != null) tsReadStream.close();
		}
	}


	// ----------------------------------------------------------------
	//
	//   Private methods
	//
	// ----------------------------------------------------------------

	private SSLSocketFactory getSSLSocketFactory(String protocol,
			String ciphers,
			KeyManager[] kms,
			TrustManager[] tms) 
					throws NoSuchAlgorithmException, KeyManagementException
	{
		// inits the SSL context
		SSLContext sslCtx = null;
		if (protocol == null) {
			sslCtx = SSLContext.getDefault();
		}
		else {
			sslCtx = SSLContext.getInstance(protocol);
			sslCtx.init(kms, tms, null);
		}

		// get the SSLSocketFactory 
		SSLSocketFactory sslSocketFactory = sslCtx.getSocketFactory();

		// wrap it
		SSLSocketFactoryWrapper sfw = new SSLSocketFactoryWrapper(sslSocketFactory, ciphers, m_options.isSslHostnameVerification());        
		return sfw;
	}


	private TrustManager[] getTrustManagers(String trustStore) 
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
	{
		InputStream tsReadStream = null;
		try{
			TrustManagerFactory tmf = null;  

			if(m_certificatesService == null){
				ServiceReference<CertificatesService> sr= m_ctx.getBundleContext().getServiceReference(CertificatesService.class);
				if(sr != null){
					m_certificatesService= m_ctx.getBundleContext().getService(sr);
				}
			}

			if(!serviceEnabled || (m_certificatesService != null && !verifyEnvironmentProperties(m_options.getSslKeystorePassword().toCharArray()))){
				return null;
			}



			// Load the default Java VM Trust Store
			tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init((KeyStore) null);

			if (trustStore != null) {

				// Load the configured the Trust Store
				File fTrustStore = new File(trustStore);
				if (fTrustStore.exists()) {

					KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
					tsReadStream = new FileInputStream(trustStore);
					ts.load(tsReadStream, null);
					tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
					tmf.init(ts);
				} else if(m_certificatesService != null){
					throw new IOException("Keystore location not correctly specified!");
				} else {
					s_logger.info("Could not find trust store at {}. Using Java default.", trustStore);
				}
			}


			return tmf.getTrustManagers();
		}
		finally{
			if(tsReadStream != null) tsReadStream.close();
		}
	}


	private KeyManager[] getKeyManagers(String keyStore,
			char[] keyStorePassword,
			String keyAlias)
					throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableEntryException
	{
		KeyStore ks = getKeyStore(keyStore, keyStorePassword, keyAlias);         
		KeyManager[] kms = null;
		if (ks != null) {
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(ks, keyStorePassword);             
			kms = kmf.getKeyManagers();
		}
		return kms; 
	}


	private KeyStore getKeyStore(String keyStore, 
			char[] keyStorePassword,
			String keyAlias) 
					throws KeyStoreException, FileNotFoundException, 
					IOException, NoSuchAlgorithmException,
					CertificateException, UnrecoverableEntryException
	{
		InputStream ksReadStream = null;
		try{
			KeyStore ks = null;

			if (keyStore != null) {

				// Load the configured the Key Store
				File fKeyStore = new File(keyStore);
				if (fKeyStore.exists()) {

					ks = KeyStore.getInstance(KeyStore.getDefaultType());
					ksReadStream = new FileInputStream(keyStore);
					ks.load(ksReadStream, null);    

					// if we have an alias, then build KeyStore with such key
					if (keyAlias != null) {                
						if (ks.containsAlias(keyAlias) && ks.isKeyEntry(keyAlias)) {
							if (ks.size() > 1) {                    
								PasswordProtection pp = new PasswordProtection(keyStorePassword);
								Entry entry = ks.getEntry(keyAlias, pp);
								ks = KeyStore.getInstance(KeyStore.getDefaultType());
								ks.load(null, null);
								ks.setEntry(keyAlias, entry, pp);
							}
						} 
						else {
							s_logger.info("Could not find alias {} in key store at {}. Using default cacert keystore.", keyAlias, keyStore);
							ks = null;
						}
					}
				} else if(m_certificatesService != null){
					throw new IOException("Keystore location not correctly specified!");
				}
				else {
					s_logger.info("Could not find key store at {}. Using Java default.", keyStore);
				}
			}
			return ks;
		}
		finally{
			if(ksReadStream != null) ksReadStream.close();
		}
	}


	private char[] getKeyStorePassword() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException 
	{
		return m_cryptoService.getKeyStorePassword(m_options.getSslTrustStore());//"eurotech".toCharArray();//return m_systemService.getJavaKeyStorePassword();
	}

	private boolean verifyEnvironmentProperties(char[] newPassword){
		try {
			loadKeyStore(m_options.getSslTrustStore(), newPassword);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static KeyStore loadKeyStore(String location, char[] password) throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException{
		FileInputStream is= null;
		try {
			is = new FileInputStream(location);
			KeyStore keystore= KeyStore.getInstance(KeyStore.getDefaultType());
			keystore.load(is, password);
			is.close();
			return keystore;
		} finally{
			if(is != null){
				is.close();
			}
		}
	}

	private static void saveKeyStore(KeyStore keystore, String location, char[] password) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException{
		FileOutputStream fos= null;
		try {
			fos = new FileOutputStream(location);
			keystore.store(fos, password);
			fos.flush();
			fos.close();
		} finally{
			if(fos != null){
				fos.close();
			}
		}
	}

	private void doUpdate(boolean onUpdate, final HashMap<String, Object> propertiesCopy) {
		// cancel a current worker handle if one if active
		if (m_handle != null) {
			m_handle.cancel(true);
		}

		m_worker = Executors.newSingleThreadExecutor();
		m_handle = m_worker.submit(new Runnable() {
			public void run() {
				s_logger.debug("--> Runner started");
				String searchedPID = (String) propertiesCopy.get(APP_PID);

				while (true) {
					s_logger.debug("--> Runner while");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					try {
						if(s_context.getServiceReference() != null && m_configurationService.getComponentConfiguration(searchedPID) != null){
							s_logger.info("Trying to update config.");
							m_configurationService.updateConfiguration(searchedPID, propertiesCopy);
							return;
						}
					} catch (KuraException e1) {
					}

				}
			}
		});
	}

	private void changeSSLKeystorePassword(char[] oldPassword, char[] newPassword) throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException{
		m_cryptoService.setKeyStorePassword(m_options.getSslTrustStore(), new String(newPassword));
		KeyStore keystore = loadKeyStore(m_options.getSslTrustStore(), oldPassword);
		saveKeyStore(keystore, m_options.getSslTrustStore(), newPassword);
	}

	private void decryptProperties(Map<String, Object> properties) {
		m_properties= new HashMap<String, Object>();
		Iterator<String> keys = properties.keySet().iterator();
		while (keys.hasNext()) {
			String key = keys.next();
			Object value = properties.get(key);
			if (key.equals(SslManagerServiceOptions.PROP_TRUST_PASSWORD)) {
				try {
					char[] decryptedPassword= m_cryptoService.decryptAes(value.toString().toCharArray());
					m_properties.put(key, decryptedPassword);
				} catch (Exception e) {
					m_properties.put(key, value.toString().toCharArray());
				} 
			}else{
				m_properties.put(key, value);
			}
		}
	}

}
