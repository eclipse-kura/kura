/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.security.keystore;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.SecureRandom;
import java.security.cert.CRL;
import java.security.cert.CertStore;
import java.security.cert.X509CRL;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.security.auth.x500.X500Principal;

import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Provides a list of APIs useful to manage and abstract key objects and certificates
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.2
 */
@ProviderType
public interface KeystoreService {

    /**
     * Returns the managed {@link KeyStore}
     *
     * @return
     * @throws KuraException
     *             when the keystore does not exist or cannot be loaded
     */
    public KeyStore getKeyStore() throws KuraException;

    /**
     * Returns the entry object specified by the provided alias
     *
     * @param alias
     * @return
     * @throws KuraException
     * @throws IllegalArgumentException
     *             if the specified alias is null
     */
    public Entry getEntry(String alias) throws KuraException;

    /**
     * Stores the specified entry with the defined alias to the managed keystore
     *
     * @param alias
     * @param entry
     * @throws KuraException
     *             if the entry could not be set or the keystore could not be persisted
     * @throws IllegalArgumentException
     *             if one of the arguments is null
     */
    public void setEntry(String alias, Entry entry) throws KuraException;

    /**
     * Returns the map representing the entries associated with the corresponding aliases in the keystore
     *
     * @return
     * @throws KuraException
     *             if the entries could not be retrieved
     */
    public Map<String, Entry> getEntries() throws KuraException;

    /**
     * Deletes the entry identified by the specified alias, if it exists.
     *
     * @param alias
     * @throws KuraException
     *             if the entry could not be deleted or the managed keystore could not be persisted after the change
     * @throws IllegalArgumentException
     *             if the specified alias is null
     */
    public void deleteEntry(String alias) throws KuraException;

    /**
     * Returns one key manager for each type of key material.
     *
     * @param algorithm
     * @return a list of key manager
     * @throws KuraException
     *             if the provided algorithm is not supported or does not exist or if the associated keystore cannot be
     *             accessed
     * @throws IllegalArgumentException
     *             if the algorithm is null
     */
    public List<KeyManager> getKeyManagers(String algorithm) throws KuraException;

    /**
     * Creates and persists a new keypair in the managed keystore using the specified alias.
     *
     * @param alias
     * @param algorithm
     * @param keySize
     * @param signatureAlgorithm
     * @param attributes
     * @throws KuraException
     *             if the keypair cannot be created or the keypair cannot be added to the managed keystore
     * @throws IllegalArgumentException
     *             if one of the arguments is null or empty
     */
    public void createKeyPair(String alias, String algorithm, int keySize, String signatureAlgorithm, String attributes)
            throws KuraException;

    /**
     * Creates and persists a new keypair in the managed keystore using the specified alias.
     *
     * @param alias
     * @param algorithm
     * @param keySize
     * @param signatureAlgorithm
     * @param attributes
     * @param secureRandom
     * @throws KuraException
     *             if the keypair cannot be created or the keypair cannot be added to the managed keystore
     * @throws IllegalArgumentException
     *             if one of the arguments is null or empty
     */
    public void createKeyPair(String alias, String algorithm, int keySize, String signatureAlgorithm, String attributes,
            SecureRandom secureRandom) throws KuraException;

    /**
     * Creates and persists a new keypair in the managed keystore using the specified alias.
     *
     * @param alias
     *            a string that will be used to identify the certificate in a key store.
     * @param algorithm
     *            a string indicating the algorithm used to generate the keypair.
     * @param algorithmParameter
     *            a set of algorithm parameters passed to the keypair generator.
     * @param signatureAlgorithm
     *            a string indicating the signature algorithm used to sign the certificate containing the generated
     *            keypair.
     * @param attributes
     *            a string representing the X.500 Distinguished Name to include in the generated certificate.
     * @param secureRandom
     *            the RNG (Random Number Generator) to use in the keypair generator.
     * @throws KuraException
     *             if the keypair cannot be created or the keypair cannot be added to the managed keystore
     * @throws IllegalArgumentException
     *             if one of the arguments is null or empty
     * 
     * @since 2.4
     */
    public void createKeyPair(String alias, String algorithm, AlgorithmParameterSpec algorithmParameter,
            String signatureAlgorithm, String attributes, SecureRandom secureRandom) throws KuraException;

    /**
     * Creates and persists a new keypair in the managed keystore using the specified alias.
     *
     * @param alias
     *            a string that will be used to identify the certificate in a key store.
     * @param algorithm
     *            a string indicating the algorithm used to generate the keypair.
     * @param algorithmParameter
     *            a set of algorithm parameters passed to the keypair generator.
     * @param signatureAlgorithm
     *            a string indicating the signature algorithm used to sign the certificate containing the generated
     *            keypair.
     * @param attributes
     *            a string representing the X.500 Distinguished Name to include in the generated certificate.
     * @throws KuraException
     *             if the keypair cannot be created or the keypair cannot be added to the managed keystore
     * @throws IllegalArgumentException
     *             if one of the arguments is null or empty
     * @since 2.4
     */
    public void createKeyPair(String alias, String algorithm, AlgorithmParameterSpec algorithmParameter,
            String signatureAlgorithm, String attributes) throws KuraException;

    /**
     * Creates and returns a CSR for the given keypair based on the provided principal and signer algorithm selected.
     *
     * @param keyPair
     *            a keypair holding the private and public key.
     * @param principal
     *            an X500Name containing the subject associated with the request we are building.
     * @param signerAlg
     *            a String representing the signer algorithm used to sign the certificate signing request.
     * @return the Certificate Signing Request in PEM format.
     * @throws KuraException
     *             if the CSR cannot be computed or if it cannot be encoded
     * @throws IllegalArgumentException
     *             if one of the arguments is null or empty
     */
    public String getCSR(KeyPair keyPair, X500Principal principal, String signerAlg) throws KuraException;

    /**
     * Creates and returns a CSR for the given keypair based on the provided principal and signer algorithm selected.
     *
     * @param alias
     *            a string that will be used to identify the entity in the keystore holding the private and public keys.
     * @param principal
     *            an X500Name containing the subject associated with the request we are building.
     * @param signerAlg
     *            a String representing the signer algorithm used to sign the certificate signing request.
     * @return the Certificate Signing Request in PEM format.
     * @throws KuraException
     *             if the alias does not correspond to a managed entry of the keystore, it refers to an entry that
     *             cannot be used to obtain a CSR or the CSR cannot be computed or encoded
     * @throws IllegalArgumentException
     *             if one of the arguments is null or empty
     */
    public String getCSR(String alias, X500Principal principal, String signerAlg) throws KuraException;

    /**
     * Creates and returns a <code>PKCS10CertificationRequestBuilder</code> for the given keypair based on the provided
     * principal and signer algorithm selected.
     *
     * @param keyPair
     *            a keypair holding the private and public key.
     * @param principal
     *            an X500Name containing the subject associated with the request we are building.
     * @return a <code>PKCS10CertificationRequestBuilder</code> that could be used to sign a Certificate Signing Request
     *         with a <code>ContentSigner</code>
     * @throws KuraException
     *             if the CSR cannot be computed or if it cannot be encoded
     * @throws IllegalArgumentException
     *             if one of the arguments is null or empty
     */
    public PKCS10CertificationRequestBuilder getCSRAsPKCS10Builder(KeyPair keyPair, X500Principal principal)
            throws KuraException;

    /**
     * Creates and returns a <code>PKCS10CertificationRequestBuilder</code> for the given keypair based on the provided
     * principal and signer algorithm selected.
     *
     * @param alias
     *            a string that will be used to identify the entity in the keystore holding the private and public keys.
     * @param principal
     *            an X500Name containing the subject associated with the request we are building.
     * @return a <code>PKCS10CertificationRequestBuilder</code> that could be used to sign a Certificate Signing Request
     *         with a <code>ContentSigner</code>
     * @throws KuraException
     *             if the alias does not correspond to a managed entry of the keystore, it refers to an entry that
     *             cannot be used to obtain a CSR or the CSR cannot be computed or encoded
     * @throws IllegalArgumentException
     *             if one of the arguments is null or empty
     */
    public PKCS10CertificationRequestBuilder getCSRAsPKCS10Builder(String alias, X500Principal principal)
            throws KuraException;

    /**
     * Returns the list of all the aliases corresponding to the keystore service managed objects
     *
     * @return
     * @throws KuraException
     *             if the list of aliases cannot be retrieved
     */
    public List<String> getAliases() throws KuraException;

    /**
     * Returns a list of the current cached CRLs.
     * 
     * @return a list of the current cached CRLs.
     * @throws KuraException
     *             if the list cannot be retrieved
     */
    public Collection<CRL> getCRLs() throws KuraException;

    /**
     * Returns a <code>CertStore</code> containing the cached CRLs.
     * 
     * @return a <code>CertStore</code> containing the cached CRLs.
     * @throws KuraException
     *             if the <code>CertStore</code> cannot be created.
     */
    public CertStore getCRLStore() throws KuraException;

    /**
     * Add a <code>X509CRL</code> to the CRLs list.
     * 
     * @param crl
     *            a <code>X509CRL</code> to be stored
     * @throws KuraException
     *             if the <code>X509CRL</code> cannot be added.
     * 
     * @since 2.4
     */
    public void addCRL(X509CRL crl) throws KuraException;

}
