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
package org.eclipse.kura.clock;

import java.util.Date;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The ClockService is used to configure how to sych the hardware clock with a remote network service.
 * The service reports when the clock has been synchronized last and raises an event when it is synchronized.
 * The ClockService is configurable to determine how the clock synchronization should happen.
 * By default, the ClockService can be configured to set the time through a Java NTP Client.
 * It can also be extended to synchronize the clock through a native Linux NTPD service,
 * using the clock acquired from a GPS signal provided by the Position Service, or
 * through the a cellular carrier.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface ClockService {

    /**
     * Returns a Date denoting when the clock was synchronized last. If the clock has
     * not yet synchronized since Kura started, null is returned.
     *
     * @return Date that the clock was last synchronized, null if not synchronized yet.
     */
    public Date getLastSync() throws KuraException;

}
