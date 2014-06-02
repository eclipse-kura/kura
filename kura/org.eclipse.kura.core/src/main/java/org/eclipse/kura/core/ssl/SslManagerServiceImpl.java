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
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

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

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.ssl.SslManagerService;
import org.eclipse.kura.ssl.SslServiceListener;
import org.eclipse.kura.system.SystemService;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslManagerServiceImpl implements SslManagerService, ConfigurableComponent
{
    private static final Logger s_logger = LoggerFactory.getLogger(SslManagerServiceImpl.class);
        
    private SystemService            m_systemService;
    private SslServiceListeners		 m_sslServiceListeners;

    @SuppressWarnings("unused")
    private ComponentContext         m_ctx;
    private SslManagerServiceOptions m_options;

    // ----------------------------------------------------------------
    //
    //   Dependencies
    //
    // ----------------------------------------------------------------

    public void setSystemService(SystemService systemService) {
        this.m_systemService = systemService;
    }

    public void unsetSystemService(SystemService systemService) {
        this.m_systemService = null;
    }
    
    // ----------------------------------------------------------------
    //
    //   Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext, Map<String,Object> properties) 
    {
        s_logger.info("activate...");
        
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
    }
        
    public void updated(Map<String,Object> properties)
    {
        s_logger.info("updated...: " + properties);

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

        String keyAlias    = null;
        String keyStore    = m_options.getSslKeyStore();
        char[] keyStorePwd = getKeyStorePassword();         
        KeyManager[]   kms = getKeyManagers(keyStore, keyStorePwd, keyAlias);
        
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

        String keyStore    = m_options.getSslKeyStore();
        char[] keyStorePwd = getKeyStorePassword();         
        KeyManager[]   kms = getKeyManagers(keyStore, keyStorePwd, keyAlias);
        
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
        // load the trust store
        String trustStore = m_options.getSslTrustStore();
        KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
        File fTrustStore = new File(trustStore);
        if (fTrustStore.exists()) {
            InputStream tsReadStream = new FileInputStream(trustStore);
            ts.load(tsReadStream, null);
        }
        else {
            ts.load(null, null);
        }
        
        // add the certificate
        ts.setCertificateEntry(alias, x509crt);
        
        // save it
        char[] trustStorePwd = getTrustStorePassword(); 
        FileOutputStream tsOutStream = new FileOutputStream(trustStore);
        ts.store(tsOutStream, trustStorePwd);
        try {
            tsOutStream.close();
        }
        catch (IOException e) {}
    }
    

    @Override
    public void deleteTrustCertificate(String alias)
        throws GeneralSecurityException, IOException
    {
        // load the trust store
        String trustStore = m_options.getSslTrustStore();
        KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream tsReadStream = new FileInputStream(trustStore);
        ts.load(tsReadStream, null);
        
        // delete the entry
        ts.deleteEntry(alias);

        // save it
        ts.store( new LoadStoreParameter() {            
            @Override
            public ProtectionParameter getProtectionParameter() {
                char[] trustStorePwd;
                try {
                    trustStorePwd = getTrustStorePassword();
                    return new PasswordProtection(trustStorePwd);
                }
                catch (Exception e) {
                    s_logger.error("Error loading TrustStore password", e);
                } 
                return null;
            }
        });        
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
        TrustManagerFactory tmf = null;        
        if (trustStore != null) {
         
            // Load the configured the Trust Store
            File fTrustStore = new File(trustStore);
            if (fTrustStore.exists()) {
                
                KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
                InputStream tsReadStream = new FileInputStream(trustStore);
                ts.load(tsReadStream, null);
                tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ts);
            }
            else {
                s_logger.info("Could not find trust store at {}. Using Java default.", trustStore);
            }
        }

        if (tmf == null) {
            // Load the default Java VM Trust Store
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
        }        
        return tmf.getTrustManagers(); 
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
        KeyStore ks = null;
        if (keyStore != null) {
         
            // Load the configured the Key Store
            File fKeyStore = new File(keyStore);
            if (fKeyStore.exists()) {
                
                ks = KeyStore.getInstance(KeyStore.getDefaultType());
                InputStream ksReadStream = new FileInputStream(keyStore);
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
                        s_logger.info("Could not find alias {} in key store at {}. Using Java default.", keyAlias, keyStore);
                        ks = null;
                    }
                }
            }
            else {
                s_logger.info("Could not find key store at {}. Using Java default.", keyStore);
            }
        }
        return ks;
    }
    
    
    private char[] getKeyStorePassword() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException 
    {
        return m_systemService.getJavaKeyStorePassword();
    }


    private char[] getTrustStorePassword() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException 
    {
        return m_systemService.getJavaTrustStorePassword();
    }
}
