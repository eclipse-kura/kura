/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.ssl;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Listener interface to be implemented by applications that needs to be notified of events in the
 * {@link SslManagerService}.
 * All registered listeners are called synchronously by the {@link SslManagerService} at the occurrence of the event.
 * It expected that implementers of this interface do NOT perform long running tasks in the implementation of this
 * interface.
 */
@ConsumerType
public interface SslServiceListener {

    /**
     * Notifies the {@link SslManagerService} has received a configuration update and it has applied the new
     * configuration
     */
    public void onConfigurationUpdated();
}
