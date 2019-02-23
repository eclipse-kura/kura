/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net;

import java.util.List;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The NetworkService allows to browse and configure the network interfaces of the system.
 * <br>
 * NetworkService extends what is offered by the standard Java APIs by offering information
 * like the NetworkInterface type - e.g. wired vs wireless vs modem - and additional information
 * regarding the address of a NetworkInterface - e.g. its getway address, DNS, and so on.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface NetworkService {

    /**
     * Returns the overall state of the networking subsystem
     */
    public NetworkState getState() throws KuraException;

    /**
     * Returns the state of a specific network interface
     */
    public NetInterfaceState getState(String interfaceName) throws KuraException;

    /**
     * Gets the names of all the network interface attached to the system.
     *
     * @return the names of all interfaces regardless of 'up' status
     */
    public List<String> getAllNetworkInterfaceNames() throws KuraException;

    /**
     * Gets the names of all the network interface attached to the system.
     * For each returned NetworkInterface, its currently active
     * InterfaceAddresses are returned.
     *
     * @return all NetworkInterfaces
     */
    public List<NetInterface<? extends NetInterfaceAddress>> getNetworkInterfaces() throws KuraException;

    /**
     * Returns the list of all available WifiAccessPoints as seen from the system.
     *
     * @return all access points accessible from the system.
     */
    public List<WifiAccessPoint> getAllWifiAccessPoints() throws KuraException;

    /**
     * Returns the list of the WifiAccessPoints visible from the specified wifi network interface.
     * If this wifiInterfaceName is in Master mode it will return a List with one WifiAccessPoint
     * which is itself.
     *
     * @param wifiInterfaceName
     *            name of the interface used to scan for the available access points
     * @return the list of the WifiAccessPoints visible from the specified wifi network interface.
     */
    public List<WifiAccessPoint> getWifiAccessPoints(String wifiInterfaceName) throws KuraException;

    /**
     * Return the active NetworkIntefaces which have active connections for the system.
     *
     * @return
     */
    public List<NetInterface<? extends NetInterfaceAddress>> getActiveNetworkInterfaces() throws KuraException;

    public String getModemUsbPort(String interfaceName);

    public String getModemPppPort(ModemDevice modemDevice) throws KuraException;
}
