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
package org.eclipse.kura.net;

import java.util.List;
import java.util.Optional;

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
     * 
     * @deprecated since 2.3
     */
    @Deprecated
    public NetworkState getState() throws KuraException;

    /**
     * Returns the state of a specific network interface
     * 
     * @deprecated since 2.3
     */
    @Deprecated
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

    /**
     * Given an interface name (e.g. 'ppp0'), look up the associated usb port
     * 
     * @param the
     *            name of the ppp interface (i.e. ppp0)
     * @return a string representing the usb port of the modem (i.e. 1-2.3)
     */
    public String getModemUsbPort(String pppInterfaceName);

    /**
     * Given a modem device, look up the associated ppp interface name
     * 
     * @param modemDevice
     * @return the name of the ppp interface
     * @throws KuraException
     */
    public String getModemPppPort(ModemDevice modemDevice) throws KuraException;

    /**
     * Given a usb path, look up the associated ppp interface name
     * 
     * @param usbPath
     *            a string representing the usb port (i.e. 1-2.3)
     * @return the name of the ppp interface
     * @throws KuraException
     * 
     * @since 2.3
     */
    public String getModemPppInterfaceName(String usbPath);

    /**
     * Given a usb path, look up the associated modem device
     * 
     * @param usbPath
     *            a string representing the usb port (i.e. 1-2.3)
     * @return the {@link ModemDevice} attached to the specified usb port
     * @throws KuraException
     * 
     * @since 2.3
     */
    public Optional<ModemDevice> getModemDevice(String usbPath);
}
