/*******************************************************************************
 * Copyright (c) 2011, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.wifi;

import org.eclipse.kura.KuraException;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Marker interface for wifi client monitoring service
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface WifiClientMonitorService {

    public void registerListener(WifiClientMonitorListener listener);

    public void unregisterListener(WifiClientMonitorListener listener);

    /**
     * Return the signal level on the given wireless interface
     * 
     * @param interfaceName
     *            the name of the wireless interface
     * @param ssid
     *            the name of the ssid the interface is attached to
     * @return an integer number representing the rssi
     * @throws KuraException
     * 
     * @deprecated since 2.4. Use {@link getSignalLevel(String interfaceName, String ssid, boolean recompute)} instead.
     */
    @Deprecated
    public int getSignalLevel(String interfaceName, String ssid) throws KuraException;

    /**
     * Return the signal level on the given wireless interface
     * 
     * @param interfaceName
     *            the name of the wireless interface
     * @param ssid
     *            the name of the ssid the interface is attached to
     * @param recompute
     *            if set to true, the rssi is recomputed. Otherwise a cached value is returned
     * @return an integer number representing the rssi
     * @throws KuraException
     * 
     * @since 2.4
     */
    public int getSignalLevel(String interfaceName, String ssid, boolean recompute) throws KuraException;
}
