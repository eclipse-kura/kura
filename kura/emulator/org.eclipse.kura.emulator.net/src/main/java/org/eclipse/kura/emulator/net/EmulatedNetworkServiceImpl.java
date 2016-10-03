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
package org.eclipse.kura.emulator.net;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.EthernetInterfaceImpl;
import org.eclipse.kura.core.net.NetInterfaceAddressImpl;
import org.eclipse.kura.emulator.Emulator;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceState;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.NetworkState;
import org.eclipse.kura.net.modem.ModemDevice;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmulatedNetworkServiceImpl implements NetworkService {

    private static final Logger s_logger = LoggerFactory.getLogger(EmulatedNetworkServiceImpl.class);

    @SuppressWarnings("unused")
    private Emulator m_emulator;

    @SuppressWarnings("unused")
    private ComponentContext m_ctx;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setEmulator(Emulator emulator) {
        this.m_emulator = emulator;
    }

    public void unsetEmulator(Emulator emulator) {
        this.m_emulator = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    protected void activate(ComponentContext componentContext) {
        //
        // save the bundle context
        this.m_ctx = componentContext;
    }

    protected void deactivate(ComponentContext componentContext) {
        this.m_ctx = null;
    }

    // ----------------------------------------------------------------
    //
    // Service APIs
    //
    // ----------------------------------------------------------------

    @Override
    public NetworkState getState() {
        java.net.InetAddress jnAddress = getFirstActiveInetAddress();
        if (jnAddress == null) {
            return NetworkState.DISCONNECTED;
        } else if (jnAddress.isLoopbackAddress() || jnAddress.isLinkLocalAddress()) {
            return NetworkState.CONNECTED_LOCAL;
        } else if (jnAddress.isSiteLocalAddress()) {
            return NetworkState.CONNECTED_SITE;
        } else {
            return NetworkState.CONNECTED_GLOBAL;
        }
    }

    @Override
    public NetInterfaceState getState(String interfaceName) {
        // Returned unknown state for the emulataed network service.
        return NetInterfaceState.UNKNOWN;
    }

    @Override
    public List<String> getAllNetworkInterfaceNames() throws KuraException {
        List<String> interfaceNames = new ArrayList<String>();

        java.net.NetworkInterface jnInterface = null;
        Enumeration<java.net.NetworkInterface> interfaces = null;
        try {
            interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                jnInterface = interfaces.nextElement();
                interfaceNames.add(jnInterface.getName());
            }
        } catch (SocketException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }

        return interfaceNames;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List<NetInterface<? extends NetInterfaceAddress>> getNetworkInterfaces() throws KuraException {
        IPAddress netAddress = null;
        NetInterfaceAddressImpl addressImpl = null;
        List<NetInterfaceAddress> addresses = null;
        List<NetInterface<? extends NetInterfaceAddress>> interfaces = new ArrayList<NetInterface<? extends NetInterfaceAddress>>();

        EthernetInterfaceImpl ethInterface = null;
        java.net.NetworkInterface jnInterface = null;
        List<java.net.InterfaceAddress> jnInterfaceAddresses = null;
        Enumeration<java.net.NetworkInterface> jnInterfaces = null;
        try {
            jnInterfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (jnInterfaces.hasMoreElements()) {

                jnInterface = jnInterfaces.nextElement();
                ethInterface = new EthernetInterfaceImpl(jnInterface.getName());
                ethInterface.setVirtual(jnInterface.isVirtual());
                ethInterface.setState(NetInterfaceState.ACTIVATED);
                ethInterface.setAutoConnect(true);

                byte[] hwAddr = null;
                boolean isUp = false;
                boolean isLoop = false;
                int mtu = 0;
                boolean isP2p = false;
                boolean multi = false;
                try {
                    hwAddr = jnInterface.getHardwareAddress();
                    isUp = jnInterface.isUp();
                    isLoop = jnInterface.isLoopback();
                    mtu = jnInterface.getMTU();
                    isP2p = jnInterface.isPointToPoint();
                    multi = jnInterface.supportsMulticast();
                } catch (Exception e) {
                    s_logger.warn("Exception while getting information for interface " + jnInterface.getName() + ": "
                            + e.getMessage());
                }
                ethInterface.setHardwareAddress(hwAddr);
                ethInterface.setLinkUp(isUp);
                ethInterface.setLoopback(isLoop);
                ethInterface.setMTU(mtu);
                ethInterface.setPointToPoint(isP2p);
                ethInterface.setSupportsMulticast(multi);
                ethInterface.setUp(isUp);

                addresses = new ArrayList<NetInterfaceAddress>();
                jnInterfaceAddresses = jnInterface.getInterfaceAddresses();
                for (java.net.InterfaceAddress jnInterfaceAddress : jnInterfaceAddresses) {

                    netAddress = IPAddress.getByAddress(jnInterfaceAddress.getAddress().getAddress());
                    addressImpl = new NetInterfaceAddressImpl();
                    addressImpl.setAddress(netAddress);
                    if (jnInterfaceAddress.getBroadcast() != null) {
                        addressImpl
                                .setBroadcast(IPAddress.getByAddress(jnInterfaceAddress.getBroadcast().getAddress()));
                    }
                    addressImpl.setNetworkPrefixLength(jnInterfaceAddress.getNetworkPrefixLength());

                    addresses.add(addressImpl);
                }
                ethInterface.setNetInterfaceAddresses(addresses);
                interfaces.add(ethInterface);
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
        return interfaces;
    }

    @Override
    public List<WifiAccessPoint> getAllWifiAccessPoints() {
        return Collections.emptyList();
    }

    @Override
    public List<WifiAccessPoint> getWifiAccessPoints(String wifiInterfaceName) {
        return Collections.emptyList();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public List<NetInterface<? extends NetInterfaceAddress>> getActiveNetworkInterfaces() throws KuraException {
        IPAddress netAddress = null;
        NetInterfaceAddressImpl addressImpl = null;
        List<NetInterfaceAddress> addresses = null;
        List<NetInterface<? extends NetInterfaceAddress>> interfaces = new ArrayList<NetInterface<? extends NetInterfaceAddress>>();

        EthernetInterfaceImpl ethInterface = null;
        java.net.NetworkInterface jnInterface = null;
        List<java.net.InterfaceAddress> jnInterfaceAddresses = null;
        Enumeration<java.net.NetworkInterface> jnInterfaces = null;
        try {
            jnInterfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (jnInterfaces.hasMoreElements()) {

                try {
                    jnInterface = jnInterfaces.nextElement();
                    if (jnInterface.isUp() && !jnInterface.isVirtual() && !jnInterface.isLoopback()
                            && !jnInterface.isPointToPoint() && jnInterface.getHardwareAddress() != null
                            && !jnInterface.getInterfaceAddresses().isEmpty()) {

                        ethInterface = new EthernetInterfaceImpl(jnInterface.getName());
                        ethInterface.setVirtual(jnInterface.isVirtual());
                        ethInterface.setState(NetInterfaceState.ACTIVATED);
                        ethInterface.setAutoConnect(true);

                        byte[] hwAddr = null;
                        boolean isUp = false;
                        boolean isLoop = false;
                        int mtu = 0;
                        boolean isP2p = false;
                        boolean multi = false;
                        try {
                            hwAddr = jnInterface.getHardwareAddress();
                            isUp = jnInterface.isUp();
                            isLoop = jnInterface.isLoopback();
                            mtu = jnInterface.getMTU();
                            isP2p = jnInterface.isPointToPoint();
                            multi = jnInterface.supportsMulticast();
                        } catch (Exception e) {
                            s_logger.warn("Exception while getting information for interface " + jnInterface.getName(),
                                    e);
                        }
                        ethInterface.setHardwareAddress(hwAddr);
                        ethInterface.setLinkUp(isUp);
                        ethInterface.setLoopback(isLoop);
                        ethInterface.setMTU(mtu);
                        ethInterface.setPointToPoint(isP2p);
                        ethInterface.setSupportsMulticast(multi);
                        ethInterface.setUp(isUp);

                        addresses = new ArrayList<NetInterfaceAddress>();
                        jnInterfaceAddresses = jnInterface.getInterfaceAddresses();
                        for (java.net.InterfaceAddress jnInterfaceAddress : jnInterfaceAddresses) {

                            netAddress = IPAddress.getByAddress(jnInterfaceAddress.getAddress().getAddress());
                            addressImpl = new NetInterfaceAddressImpl();
                            addressImpl.setAddress(netAddress);
                            if (jnInterfaceAddress.getBroadcast() != null) {
                                addressImpl.setBroadcast(
                                        IPAddress.getByAddress(jnInterfaceAddress.getBroadcast().getAddress()));
                            }
                            addressImpl.setNetworkPrefixLength(jnInterfaceAddress.getNetworkPrefixLength());

                            addresses.add(addressImpl);
                        }
                        ethInterface.setNetInterfaceAddresses(addresses);
                        interfaces.add(ethInterface);
                    }
                } catch (SocketException se) {
                    s_logger.warn("Exception while getting information for interface " + jnInterface.getName(), se);
                }
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
        return interfaces;
    }

    // ---------------------------------------------------------
    //
    // Private methods
    //
    // ---------------------------------------------------------

    private java.net.InetAddress getFirstActiveInetAddress() {
        java.net.InetAddress jnAddress = null;
        java.net.NetworkInterface jnInterface = null;
        try {

            Enumeration<java.net.NetworkInterface> interfaces = null;

            // search for a non loopback interface
            interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {

                jnInterface = interfaces.nextElement();
                if (jnInterface.isUp() && !jnInterface.isLoopback() && !jnInterface.isVirtual()) {

                    Enumeration<java.net.InetAddress> addresses = null;
                    addresses = jnInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {

                        java.net.InetAddress address = addresses.nextElement();
                        if (address instanceof java.net.Inet4Address && !address.isLoopbackAddress()) {
                            jnAddress = address;
                            break;
                        }
                    }
                }
                if (jnAddress != null) {
                    break;
                }

                // get a loopback interface
                interfaces = java.net.NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {

                    jnInterface = interfaces.nextElement();
                    if (jnInterface.isUp() && !jnInterface.isVirtual()) {

                        Enumeration<java.net.InetAddress> addresses = null;
                        addresses = jnInterface.getInetAddresses();
                        while (addresses.hasMoreElements()) {

                            java.net.InetAddress address = addresses.nextElement();
                            if (address instanceof java.net.Inet4Address) {
                                jnAddress = address;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            s_logger.error("Error getting IP address", e);
        }
        return jnAddress;
    }

    @Override
    public String getModemUsbPort(String interfaceName) {
        return null;
    }

    @Override
    public String getModemPppPort(ModemDevice modemDevice) throws KuraException {
        return null;
    }
}
