package org.eclipse.kura.db.keyvalue;

import org.eclipse.kura.KuraConnectException;

/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * {@link KeyValueDbService} provides APIs to use the functionalities a of a KeyValue database.
 *
 * @since 2.5.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface KeyValueDbService {

    /**
     * Connects to the configured KeyValue database.
     */
    public void connect() throws KuraConnectException;

    /**
     * Immediately close the connection from the configured KeyValue database.
     */
    public void close();

    /**
     * Close the connection to the configured KeyValue database.
     * 
     * @param quiesceTimeout
     *            the quiet period to disconnect gracefully.
     * 
     */
    public void close(long quiesceTimeout);

    /**
     * Returns whether the connection to the database is closed or not.
     */
    public boolean isClose();

    /**
     * Returns whether the database is connected or not.
     */
    public boolean isConnected();

    /**
     * Set a key with the specified value in the database
     * 
     * @param key
     *            the key name
     * @param value
     *            the value of the key as array of byte
     * @throws KuraException
     *             if the operation is unsuccessful
     */
    void set(String key, byte[] value) throws KuraException;

    /**
     * Set a key with the specified value in the database
     * 
     * @param key
     *            the key name
     * @param value
     *            the value of the key as a String. The string encondig is platform dependent
     * @throws KuraException
     *             if the operation is unsuccessful
     */
    void set(String key, String value) throws KuraException;

    /**
     * Get a key with the specified value in the database
     * 
     * @param key
     *            the key name
     * 
     * @return the key value as byte array
     * @throws KuraException
     *             if the operation is unsuccessful
     */
    byte[] get(String key) throws KuraException;

    /**
     * Get a key with the specified value in the database
     * 
     * @param key
     *            the key name
     * 
     * @return the key value as String. The string encondig is platform dependent
     * @throws KuraException
     *             if the operation is unsuccessful
     */
    String getAsString(String key) throws KuraException;

    /**
     * Delete a key with the specified value in the database
     * 
     * @param key
     *            the key name
     * 
     * @throws KuraException
     *             if the operation is unsuccessful
     */
    void delete(String key) throws KuraException;
}
