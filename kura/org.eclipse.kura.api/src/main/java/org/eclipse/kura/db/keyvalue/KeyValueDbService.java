package org.eclipse.kura.db.keyvalue;

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
import org.eclipse.kura.connection.listener.ConnectionListener;
import org.osgi.annotation.versioning.ProviderType;

/**
 * {@link KeyValueDbService} provides APIs to use the functionalities a of a database capable of storing data in form of key-value pairs.
 *
 * @since 2.5.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface KeyValueDbService {

    /**
     * Adds a {@link ConnectionListener}.
     *
     * @param listener
     *            to add
     *
     */
    public void addListener(ConnectionListener listener);

    /**
     * Removes a {@link ConnectionListener}
     *
     * @param listener
     *            to remove
     *
     */
    public void removeListener(ConnectionListener listener);

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
