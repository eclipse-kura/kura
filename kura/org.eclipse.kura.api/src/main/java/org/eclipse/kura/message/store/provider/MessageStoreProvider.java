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
package org.eclipse.kura.message.store.provider;

import org.eclipse.kura.KuraStoreException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a service that allows to create {@link MessageStore} instances.
 * 
 * @since 2.5
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface MessageStoreProvider {

    /**
     * Opens or creates a {@link MessageStore} instance with the given name.
     * 
     * @param name     the store name.
     * @param capacity the maximum message count that is possible to store.
     * @return
     * @throws KuraStoreException
     */
    public MessageStore openMessageStore(String name, int capacity) throws KuraStoreException;
}
