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

package org.eclipse.kura.connection.listener;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Listener interface to be implemented by applications that needs to be notified about connection events.
 * All registered listeners are called sequentially by the implementations at the occurrence of the
 * event.
 *
 * @since 2.5.0
 */
@ConsumerType
public interface ConnectionListener {

    /**
     * Notifies the listener that the connection has been established.
     */
    public void connected();

    /**
     * Notifies the listener that the connection has been closed.
     */
    public void disconnected();
}
