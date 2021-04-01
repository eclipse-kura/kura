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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import javax.net.ssl.KeyManager;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * @since 2.2
 */
@ProviderType
public interface KeystoreService {

    /**
     * Returns the managed {@link KeyStore}
     *
     * @return
     */
    public KeyStore getKeyStore() throws GeneralSecurityException, IOException;

    public void setEntry(String alias, Entry entry) throws GeneralSecurityException, IOException;

    /**
     * Returns the entry object specified by the provided alias
     *
     * @param alias
     * @return the entry specified by the provided argument
     * @throws IllegalArgumentException
     *             if the alias is null
     */
    public Entry getEntry(String alias) throws GeneralSecurityException, IOException;

    /**
     * Returns the map representing the entries associated with the corresponding aliases in the keystore
     *
     * @return
     */
    public Map<String, Entry> getEntries() throws GeneralSecurityException, IOException;

    public void deleteEntry(String alias) throws GeneralSecurityException, IOException;

    /**
     * Returns one key manager for each type of key material.
     *
     * @param algorithm
     * @return a list of key manager
     * @throws GeneralSecurityException
     *             if the provided algorithm is not supported or does not exist
     * @throws IOException
     *             if the associated keystore cannot be accessed
     * @throws IllegalArgumentException
     *             if the algorithm is null
     */
    public List<KeyManager> getKeyManagers(String algorithm) throws GeneralSecurityException, IOException;

    public KeyPair createKeyPair(String alias, String algorithm, int keySize, String dn, int validity,
            String sigAlgName) throws KuraException;

    public KeyPair createKeyPair(String alias, String algorithm, int keySize, String dn, int validity,
            String sigAlgName, SecureRandom secureRandom) throws KuraException;

    public String getCSR(String alias);

    public List<String> getAliases() throws GeneralSecurityException, IOException;

}
