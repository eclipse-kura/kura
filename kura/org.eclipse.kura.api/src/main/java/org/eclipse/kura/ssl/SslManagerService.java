/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.ssl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLSocketFactory;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The SslManagerService is responsible to manage the configuration of the SSL connections.
 * It provide APIs to manage the trust certificates and the private keys and public certificates
 * and enforces best practices that are not enabled by default in the Java VM.
 * For example, it enables Hostname Verification, disables the legacy SSL-2.0-compatible Client Hello,
 * and disable the Nagle algorithm.
 * Its implementation is configurable exposing the possibility to express the allowed SSL protocols,
 * the allowed cipher suites, and the location of the Trust Store and the Key Store files.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface SslManagerService {

    /**
     * Returns an SSLSocketFactory based on the current configuration of the SslManagerService and applying
     * best practices like Hostname Verification and disables the legacy SSL-2.0-compatible Client Hello.<br>
     * If the SslManagerService configuration contains a path to a custom Trust store, then it will be used.
     * If not, the Java VM default Trust Store will be used.<br>
     * If the SslManagerService configuration contains a path to a custom Key store, then it will be used.
     * If not, no Key store will be specified..<br>
     *
     * @return the SSLSocketFactory
     */
    public SSLSocketFactory getSSLSocketFactory() throws GeneralSecurityException, IOException;

    /**
     * Returns an SSLSocketFactory based on the current configuration of the SslManagerService and applying
     * best practices like Hostname Verification and disables the legacy SSL-2.0-compatible Client Hello.<br>
     * If the SslManagerService configuration contains a path to a custom Trust store, then it will be used.
     * If not, the Java VM default Trust Store will be used.<br>
     * If the SslManagerService configuration contains a path to a custom Key store, and such store contains
     * a KeyEntry with the alias "keyAlias" then a KeyStore with the only such KeyEntry will be used.
     * If no custom store is configured or it does not contain the "keyAlias" specified, no Key store will be
     * specified..<br>
     *
     * @param keyAlias
     *            alias of the entry in the KeyStore to be used for the returned SSLSocketFactory
     * @return the SSLSocketFactory
     */
    public SSLSocketFactory getSSLSocketFactory(String keyAlias) throws GeneralSecurityException, IOException;

    /**
     * Returns an SSLSocketFactory based on the specified parameters and applying best practices
     * like Hostname Verification (enabled by default) and disables the legacy SSL-2.0-compatible Client Hello.<br>
     *
     * @param protocol
     *            the protocol to use to initialize the SSLContext - e.g. TLSv1.2
     * @param cipherSuites
     *            allowed cipher suites for the returned SSLSocketFactory
     * @param trustStorePath
     *            Location of the Java keystore file containing the collection of CA certificates trusted by this
     *            application process (trust store). Key store type is expected to be JKS.
     * @param keyStorePath
     *            Location of the Java keystore file containing an application process's own certificate and private
     *            key. Key store type is expected to be JKS.
     * @param keyStorePassword
     *            Password to access the private key from the keystore file.
     * @param keyAlias
     *            alias of the entry in the KeyStore to be used for the returned SSLSocketFactory
     * @return the SSLSocketFactory
     */
    public SSLSocketFactory getSSLSocketFactory(String protocol, String cipherSuites, String trustStorePath,
            String keyStorePath, char[] keyStorePassword, String keyAlias) throws GeneralSecurityException, IOException;

    /**
     * Returns an SSLSocketFactory based on the specified parameters and applying best practices
     * like Hostname Verification and disables the legacy SSL-2.0-compatible Client Hello.<br>
     *
     * @param protocol
     *            the protocol to use to initialize the SSLContext - e.g. TLSv1.2
     * @param cipherSuites
     *            allowed cipher suites for the returned SSLSocketFactory
     * @param trustStorePath
     *            Location of the Java keystore file containing the collection of CA certificates trusted by this
     *            application process (trust store). Key store type is expected to be JKS.
     * @param keyStorePath
     *            Location of the Java keystore file containing an application process's own certificate and private
     *            key. Key store type is expected to be JKS.
     * @param keyStorePassword
     *            Password to access the private key from the keystore file.
     * @param keyAlias
     *            alias of the entry in the KeyStore to be used for the returned SSLSocketFactory
     * @param hostnameVerification
     *            enable server Hostname Verification
     * @return the SSLSocketFactory
     */
    public SSLSocketFactory getSSLSocketFactory(String protocol, String cipherSuites, String trustStorePath,
            String keyStorePath, char[] keyStorePassword, String keyAlias, boolean hostnameVerification)
                    throws GeneralSecurityException, IOException;

    /**
     * Returns the X509 Certificates installed in the currently configured trust store.
     * If the SslManagerService configuration contains a path to a custom trust store, then the returned certificates
     * are the ones installed in such store.
     * Otherwise the default Java VM trust store will be listed.
     *
     * @return the X509Certificates
     */
    public X509Certificate[] getTrustCertificates() throws GeneralSecurityException, IOException;

    /**
     * Installs the specified X509 certificate in the currently configured trust store.
     * If the SslManagerService configuration contains a path to a custom trust store, then the certificate will be
     * installed in such store.
     * Otherwise the certificate will be installed in the default Java VM trust store.
     *
     * @param x509crt
     *            certificate to be installed
     */
    public void installTrustCertificate(String alias, X509Certificate x509crt)
            throws GeneralSecurityException, IOException;

    /**
     * Deletes the X509 certificate with the specified Common Name (cn) from the currently configured trust store.
     * If the SslManagerService configuration contains a path to a custom trust store, then the certificate will be
     * deleted from such store.
     * Otherwise the certificate will be deleted from the default Java VM trust store.
     *
     * @param alias
     */
    public void deleteTrustCertificate(String alias) throws GeneralSecurityException, IOException;

    /**
     * Installs a private key and the correspondent public certificate chains in the configured key store with the
     * defined alias.
     *
     * @param alias
     *            that is a string that will be used to identify the certificates in the key store
     * @param privateKey
     *            that represents PrivateKey object
     * @param password
     *            that represents the password used to encode the keys in the key store
     * @param publicCerts
     *            that represents an array of Certificate objects that contain the public certificate chain
     *
     */
    public void installPrivateKey(String alias, PrivateKey privateKey, char[] password, Certificate[] publicCerts)
            throws GeneralSecurityException, IOException;
}
