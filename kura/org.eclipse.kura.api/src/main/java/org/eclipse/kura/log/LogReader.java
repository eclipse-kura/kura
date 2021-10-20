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

import org.eclipse.kura.log.listener.LogReaderListener;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The LogReader interface is implemented by all the services responsible to read logs from the system, filesystem or
 * processes running on the system.
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 1.0
 */
@ProviderType
public interface LogReader {

    /**
     * Registers a {@link LogReaderListener} that will be notified of new log events
     * 
     * @param listener
     *            a {@link LogReaderListener}
     */
    public void registerLogListener(LogReaderListener listener);

    /**
     * Unregisters a {@link LogReaderListener} from the list of log events listeners
     * 
     * @param listener
     *            the {@link LogReaderListener} to unregister
     */
    public void unregisterLogListener(LogReaderListener listener);
}
