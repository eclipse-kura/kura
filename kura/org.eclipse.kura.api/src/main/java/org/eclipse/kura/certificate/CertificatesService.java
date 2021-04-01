/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.certificate;

import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.message.KuraApplicationTopic;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The CertificatesService is used to manage the storage, listing and retrieval of public certificates
 * from a key store.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface CertificatesService {

    /**
     * The storeCertificate interface method receives a certificate and an alias that should be stored in a key store
     *
     * @param cert
     *            The certificate of type Certificate that has to be stored in a key store
     * @param alias
     *            A string that will be used to identify the certificate in a key store
     * @throws KuraException
     *             raised if the certificate storage operation failed
     * @deprecated
     */
    @Deprecated
    public void storeCertificate(Certificate cert, String alias) throws KuraException;

    /**
     * listCACertificatesAliases provides an enumeration of strings representing the different CA certificates
     * stored in a key store
     *
     * @return An enumeration containing the strings that represent the CA aliases stored in a key store.
     *
     * @deprecated
     */
    @Deprecated
    public Enumeration<String> listCACertificatesAliases();

    /**
     * listSSLCertificatesAliases provides an enumeration of strings representing the different ssl certificates
     * stored in a key store
     *
     * @return An enumeration containing the strings that represent the aliases stored in a key store.
     *
     * @deprecated
     */
    @Deprecated
    public Enumeration<String> listSSLCertificatesAliases();

    /**
     * listDMCertificatesAliases provides an enumeration of strings representing the different certificates used to
     * authenticate
     * the messages coming from the remote platform and stored in the device key store
     *
     * @return An enumeration containing the strings that represent the aliases stored in a key store.
     *
     * @deprecated
     */
    @Deprecated
    public Enumeration<String> listDMCertificatesAliases();

    /**
     * listBundleCertificatesAliases provides an enumeration of strings representing the different certificates used to
     * sign
     * the bundles and that are stored in the device key store
     *
     * @return An enumeration containing the strings that represent the aliases stored in a key store.
     *
     * @deprecated
     */
    @Deprecated
    public Enumeration<String> listBundleCertificatesAliases();

    /**
     * returnCertificate returns the certificate corresponding to the specified alias.
     *
     * @param alias
     *            The string used to identify the certificate in a key store
     * @return A Certificate object retrieved from a key store.
     *
     * @deprecated
     */
    @Deprecated
    public Certificate returnCertificate(String alias) throws KuraException;

    /**
     * removeCertificate tries to remove the specified certificate from the key store. Returns true, if the removal
     * operation succeeded. False, otherwise.
     *
     * @param alias
     *            The string used to identify the certificate in a key store
     * @throws KuraException
     *             raised if the certificate removal operation failed
     *
     * @deprecated
     */
    @Deprecated
    public void removeCertificate(String alias) throws KuraException;

    /**
     * verifySignature is a method that takes the topic used
     * to send the message and the signed message to verify the correctness of the signature.
     *
     * @param kuraAppTopic
     *            The application topic part used to send the message
     * @param kuraPayload
     *            The kuraPayload message received and that needs to be verified
     * @return A boolean value that is true if the signature received corresponds with the signature
     *         calculated from the message content. False otherwise.
     * @since 2.0
     *
     */
    public boolean verifySignature(KuraApplicationTopic kuraAppTopic, KuraPayload kuraPayload);

    /**
     * 
     * @return
     * @throws KuraException
     * 
     * @since 2.2
     */
    public Set<CertificateInfo> listStoredCertificates() throws KuraException;

    /**
     * 
     * @param alias
     * @param privateKey
     * @param password
     * @param publicCerts
     * @throws KuraException
     * 
     * @since 2.2
     */
    public void addPrivateKey(KuraPrivateKey privateKey) throws KuraException;

    /**
     * Return the list of the installed {@KuraCertificate}
     * 
     * @return a list of {@KuraCertificate}
     * @throws KuraException
     * 
     * @since 2.2
     */
    public List<KuraCertificate> getCertificates() throws KuraException;

    /**
     * Return the {@KuraCertificate} identified by its id
     * 
     * @param id
     *            the id of the certificate
     * @return the {@KuraCertificate}
     * @throws KuraException
     * 
     * @since 2.2
     */
    public KuraCertificate getCertificate(String id) throws KuraException;

    /**
     * Update the {@KuraCertificate} in a keystore
     * 
     * @param id
     *            the id of the certificate
     * @param certificate
     *            the new certificate
     * @throws KuraException
     * 
     * @since 2.2
     */
    public void updateCertificate(KuraCertificate certificate) throws KuraException;

    /**
     * Add a {@KuraCertificate} in a keystore
     * 
     * @param certificate
     *            the new certificate
     * @throws KuraException
     * 
     * @since 2.2
     */
    public void addCertificate(KuraCertificate certificate) throws KuraException;

    /**
     * Delete the certificate identified by its id
     * 
     * @param id
     *            the id of the certificate
     * @throws KuraException
     * 
     * @since 2.2
     */
    public void deleteCertificate(String id) throws KuraException;

}
