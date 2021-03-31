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
import java.util.List;

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
     * @return
     */
    public KeyStore getKeyStore() throws GeneralSecurityException, IOException;
    
    /**
     * Returns the key pair specified by the provided id
     * @param id
     * @return the kay pair specified by the provided argument
     * @throws IllegalArgumentException if the id is null
     */
    public KeyPair getKeyPair(String id) throws GeneralSecurityException, IOException;
    
    /**
     * Returns the list of all the managed keyPairs
     * @return
     */
    public List<KeyPair> getKeyPairs() throws GeneralSecurityException, IOException;
    
    /**
     * Returns one key manager for each type of key material.
     * @param algorithm
     * @return a list of key manager
     * @throws GeneralSecurityException if the provided algorithm is not supported or does not exist
     * @throws IOException if the associated keystore cannot be accessed
     * @throws IllegalArgumentException if the algorithm is null
     */
    public List<KeyManager> getKeyManagers(String algorithm) throws GeneralSecurityException, IOException;
    
    public KeyPair createKeyPair(String id) throws KuraException;
    
    public void deleteEntry(String id) throws GeneralSecurityException, IOException;
    
    public String getCSR(String id);
    

}
