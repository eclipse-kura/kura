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

import org.eclipse.kura.KuraStoreException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a service that allows to create {@link WireRecordStore} instances.
 * 
 * @since 2.5
 * @noextend This class is not intended to be extended by clients.
 */
@ProviderType
public interface WireRecordStoreProvider {

    /**
     * Opens or creates a {@link WireRecordStore} instance with the given name.
     * 
     * @param name the store name
     * @return the result {@link WireRecordStore}.
     * @throws KuraStoreException
     */
    public WireRecordStore openWireRecordStore(String name) throws KuraStoreException;

}
