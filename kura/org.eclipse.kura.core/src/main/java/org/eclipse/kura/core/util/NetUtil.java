/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetUtil {

    private static final Logger s_logger = LoggerFactory.getLogger(NetUtil.class);

    public static String hardwareAddressToString(byte[] macAddress) {
        if (macAddress == null) {
            return "N/A";
        }

        if (macAddress.length != 6) {
            throw new IllegalArgumentException("macAddress is invalid");
        }

        StringJoiner sj = new StringJoiner(":");
        for (byte item : macAddress) {
        	sj.add(String.format("%02X", item));
        }
        
        return sj.toString();
    }

    public static byte[] hardwareAddressToBytes(String macAddress) {
        if (macAddress == null || macAddress.isEmpty()) {
            return new byte[] { 0, 0, 0, 0, 0, 0 };
        }
        
        String[] items = macAddress.split("\\:");

        if (items.length != 6) {
            throw new IllegalArgumentException("mac is invalid: " + macAddress);
        }

        byte[] bytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            String item = items[i];
            if (item.isEmpty() || (item.length() > 2)) {
                throw new IllegalArgumentException("mac is invalid: " + macAddress);
            }

            try {
                bytes[i] = (byte) Integer.parseInt(items[i], 16);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("mac is invalid: " + macAddress, e);
            }
        }

        return bytes;
    }

    public static String getPrimaryMacAddress() {
        NetworkInterface firstInterface = null;
        Enumeration<NetworkInterface> nifs = null;
        try {

            // look for eth0 or en0 first
            nifs = NetworkInterface.getNetworkInterfaces();
            if (nifs != null) {
                while (nifs.hasMoreElements()) {
                    NetworkInterface nif = nifs.nextElement();
                    if ("eth0".equals(nif.getName()) || "en0".equals(nif.getName())) {
                        return hardwareAddressToString(nif.getHardwareAddress());
                    }
                }
            }

            // if not found yet, look for the first active ethernet interface
            nifs = NetworkInterface.getNetworkInterfaces();
            if (nifs != null) {
                while (nifs.hasMoreElements()) {
                    NetworkInterface nif = nifs.nextElement();
                    if (!nif.isVirtual() && nif.getHardwareAddress() != null) {
                        firstInterface = nif;
                        if (nif.getName().startsWith("eth") || nif.getName().startsWith("en")) {
                            return hardwareAddressToString(nif.getHardwareAddress());
                        }
                    }
                }
            }

            if (firstInterface != null) {
                return hardwareAddressToString(firstInterface.getHardwareAddress());
            }
        } catch (Exception e) {
            s_logger.warn("Exception while getting current IP", e);
        }

        return null;
    }

    public static InetAddress getCurrentInetAddress() {
        try {

            Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
            if (nifs != null) {

                while (nifs.hasMoreElements()) {

                    NetworkInterface nif = nifs.nextElement();
                    if (!nif.isLoopback() && nif.isUp() && !nif.isVirtual() && nif.getHardwareAddress() != null) {

                        Enumeration<InetAddress> nadrs = nif.getInetAddresses();
                        while (nadrs.hasMoreElements()) {

                            InetAddress adr = nadrs.nextElement();
                            if (adr != null && !adr.isLoopbackAddress()
                                    && (nif.isPointToPoint() || !adr.isLinkLocalAddress())) {
                                return adr;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            s_logger.warn("Exception while getting current IP", e);
        }
        return null;
    }
}
