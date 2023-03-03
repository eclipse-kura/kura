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

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Listener interface to be implemented by applications that needs to be notified about connection events in the
 * {@link MessageStore}.
 * All registered listeners are called synchronously by the {@link MessageStore} implementation at the occurrence of the
 * event.
 *
 * @since 2.5.0
 */
@ConsumerType
public interface ConnectionListener {

    /**
     * Notifies the listener that the connection to the {@link MessageStore} has been established.
     */
    public void connected();

    /**
     * Notifies the listener that the connection to the {@link MessageStore} has been closed.
     */
    public void disconnected();
}
