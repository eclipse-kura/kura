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
 ******************************************************************************/
package org.eclipse.kura.wire.store.provider;

import java.util.List;

import org.eclipse.kura.KuraStoreException;
import org.eclipse.kura.wire.WireRecord;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a store that is capable to perform queries defined in an
 * implementation specific language and return the result in terms of a list of
 * {@link WireRecord}s.
 * 
 * @since 2.5
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface WireRecordStore {

    /**
     * Removes all records in the store except the most recent
     * <code>noOfRecordsToKeep</code>.
     * 
     * @param noOfRecordsToKeep
     *            the no of records to keep in the table
     * @throws KuraStoreException
     */
    public void truncate(int noOfRecordsToKeep) throws KuraStoreException;

    /**
     * Returns the number of records currently in the store.
     * 
     * @return the no of records currently in the store
     * @throws KuraStoreException
     */
    public int getSize() throws KuraStoreException;

    /**
     * Insert the provided list of {@link WireRecord} instances in the store.
     * 
     * @param records
     *            the list of records to be inserted
     * @throws KuraStoreException
     */
    public void insertRecords(List<WireRecord> records) throws KuraStoreException;

    /**
     * 
     * Closes the store, releasing any runtime resource allocated for it.
     */
    public void close();
}
