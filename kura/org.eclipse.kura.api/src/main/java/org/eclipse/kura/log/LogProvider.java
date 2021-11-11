/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.log;

import org.eclipse.kura.log.listener.LogListener;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The LogProvider interface is implemented by all the services responsible to notify {@link LogListener}.
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 2.3
 */
@ProviderType
public interface LogProvider {

    /**
     * Registers a {@link LogListener} that will be notified of new log events
     * 
     * @param listener
     *            a {@link LogListener}
     */
    public void registerLogListener(LogListener listener);

    /**
     * Unregisters a {@link LogListener} from the list of log events listeners
     * 
     * @param listener
     *            the {@link LogListener} to unregister
     */
    public void unregisterLogListener(LogListener listener);

}
