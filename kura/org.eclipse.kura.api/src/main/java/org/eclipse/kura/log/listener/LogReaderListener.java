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
package org.eclipse.kura.log.listener;

import org.eclipse.kura.log.LogEntry;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Listener interface to be implemented by applications that needs to be notified of events in the {@link LogReader}
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 1.0
 */
@ProviderType
public interface LogReaderListener {

    /**
     * Notifies the listener that a new log entry has been received.
     */
    public void newLogEntry(LogEntry entry);
}
