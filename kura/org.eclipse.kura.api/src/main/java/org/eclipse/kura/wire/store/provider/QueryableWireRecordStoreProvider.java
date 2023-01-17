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
 * @noextend This class is not intended to be extended by clients.
 */
@ProviderType
public interface QueryableWireRecordStoreProvider {

    /**
     * Perform the given query specified in an implementation defined language.
     * 
     * @param query
     *              the query to be run
     * @return a List of WireRecords that contains the result of the query
     * @throws KuraStoreException
     */
    public List<WireRecord> performQuery(String query) throws KuraStoreException;
}
