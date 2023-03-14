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

package org.eclipse.kura.store.listener;

import org.eclipse.kura.message.store.provider.MessageStoreProvider;
import org.eclipse.kura.wire.store.provider.WireRecordStoreProvider;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Listener interface to be implemented by applications that needs to be notified about connection events in the
 * {@link MessageStoreProvider} and {@link WireRecordStoreProvider}.
 * All registered listeners are called sequentially by the implementations at the occurrence of the
 * event.
 *
 * @since 2.5.0
 */
@ConsumerType
public interface ConnectionListener {

    /**
     * Notifies the listener that the connection to the {@link MessageStoreProvider} or {@link WireRecordStoreProvider}
     * has been established.
     */
    public void connected();

    /**
     * Notifies the listener that the connection to the {@link MessageStoreProvider} or {@link WireRecordStoreProvider}
     * has been closed.
     */
    public void disconnected();
}
