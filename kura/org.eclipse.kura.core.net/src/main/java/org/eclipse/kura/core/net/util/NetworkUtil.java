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
package org.eclipse.kura.core.net.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetworkUtil {

    private static final Logger s_logger = LoggerFactory.getLogger(NetworkUtil.class);
    
    private NetworkUtil() {
    }

    public static String calculateNetwork(String ipAddress, String netmask) {
        int ipAddressValue = NetworkUtil.convertIp4Address(ipAddress);
        int netmaskValue = NetworkUtil.convertIp4Address(netmask);

        int network = ipAddressValue & netmaskValue;
        return dottedQuad(network);
    }

    public static String calculateBroadcast(String ipAddress, String netmask) {
        int ipAddressValue = NetworkUtil.convertIp4Address(ipAddress);
        int netmaskValue = NetworkUtil.convertIp4Address(netmask);

        int network = ipAddressValue | ~netmaskValue;
        return dottedQuad(network);
    }

    public static String getNetmaskStringForm(int prefix) {
        if (prefix >= 1 && prefix <= 32) {
            int mask = ~((1 << 32 - prefix) - 1);
            return dottedQuad(mask);
        } else {
            throw new IllegalArgumentException("prefix is invalid: " + Integer.toString(prefix));
        }
    }

    public static short getNetmaskShortForm(String netmask) {
        if (netmask == null) {
            throw new IllegalArgumentException("netmask is null");
        }

        int netmaskValue = NetworkUtil.convertIp4Address(netmask);

        boolean hitZero = false;
        int displayMask = 1 << 31;
        int count = 0;

        for (int c = 1; c <= 32; c++) {
            if ((netmaskValue & displayMask) == 0) {
                hitZero = true;
            } else {
                if (hitZero) {
                    s_logger.error("received invalid mask: " + netmask);
                    throw new IllegalArgumentException("netmask is invalid: " + netmask);
                }

                count++;
            }

            netmaskValue <<= 1;
        }

        return (short) count;
    }

    public static String dottedQuad(int ip) {
        String[] items = new String[4];
        for (int i = 3; i >= 0; i--) {
            int value = ip & 0xFF;
            items[i] = Integer.toString(value);
            ip = ip >>> 8;
        }

        return String.join(".", items);
    }

    public static int convertIp4Address(String ipAddress) {
        if (ipAddress == null) {
            throw new IllegalArgumentException("ipAddress is null");
        }

        String[] splitIpAddress = ipAddress.split("\\.");

        if (splitIpAddress.length != 4) {
            throw new IllegalArgumentException("ipAddress is invalid: " + ipAddress);
        }

        short[] addressBytes = new short[4];

        for (int i = 0; i < 4; i++) {
            String octet = splitIpAddress[i];
            addressBytes[i] = Short.parseShort(octet);
        }

        return NetworkUtil.packIp4AddressBytes(addressBytes);
    }

    public static int packIp4AddressBytes(short[] bytes) {
        if ((bytes == null) || (bytes.length != 4)) {
            throw new IllegalArgumentException("bytes is null or invalid");
        }

        int val = 0;
        for (int i = 0; i < 4; i++) {
            if ((bytes[i] < 0) || (bytes[i] > 255)) {
                throw new IllegalArgumentException(
                        "bytes is invalid; value is out of range: " + Integer.toString(bytes[i]));
            }

            val = val << 8;
            val |= bytes[i];
        }
        return val;
    }

    public static short[] unpackIP4AddressInt(int address) {
        short[] addressBytes = new short[4];
        int value = address;

        for (int i = 3; i >= 0; i--) {
            addressBytes[i] = (short) (value & 0xFF);
            value = value >>> 8;
        }

        return addressBytes;
    }

    public static byte[] convertIP6Address(String fullFormIP6Address) {
        if (fullFormIP6Address == null) {
            throw new IllegalArgumentException("fullFormIP6Address is null");
        }

        byte[] retVal = new byte[16];
        String[] ip6Split = fullFormIP6Address.split(":");

        if (ip6Split.length != 8) {
            throw new IllegalArgumentException("fullFormIP6Address is invalid: " + fullFormIP6Address);
        }

        for (int i = 0; i < 8; i++) {
            try {
                String octet = ip6Split[i];
                int value = Integer.parseInt(octet, 16);

                if ((value < 0) || (value > 0xFFFF)) {
                    throw new IllegalArgumentException("fullFormIP6Address is invalid: " + fullFormIP6Address);
                }

                int k = i * 2;
                retVal[k] = (byte) ((value >>> 8) & 0xFF);
                retVal[k + 1] = (byte) (value & 0xFF);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("fullFormIP6Address is invalid: " + fullFormIP6Address, e);
            }
        }

        return retVal;
    }

    public static String convertIP6Address(byte[] bytes) {
        if ((bytes == null) || (bytes.length != 16)) {
            throw new IllegalArgumentException("bytes is null or invalid");
        }

        String[] items = new String[8];
        for (int i = 0; i < 8; i++) {
            int k = i * 2;
            int value = (bytes[k] << 8) & 0xFF00;
            value |= bytes[k + 1] & 0xFF;
            items[i] = Integer.toHexString(value);
        }

        return String.join(":", items);
    }

    public static String macToString(byte[] mac) {
        if ((mac == null) || (mac.length != 6)) {
            throw new IllegalArgumentException("mac is null or invalid");
        }

        String[] items = new String[6];
        for (int i = 0; i < 6; i++) {
            String octet = Integer.toHexString(mac[i] & 0xFF).toUpperCase();
            if (octet.length() == 1) {
                octet = "0" + octet;
            }

            items[i] = octet;
        }

        return String.join(":", items);
    }

    public static byte[] macToBytes(String mac) {
        if (mac == null) {
            throw new IllegalArgumentException("mac is null");
        }

        String[] items = mac.split("\\:");

        if (items.length != 6) {
            throw new IllegalArgumentException("mac is invalid: " + mac);
        }

        byte[] bytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            String item = items[i];
            if (item.isEmpty() || (item.length() > 2)) {
                throw new IllegalArgumentException("mac is invalid: " + mac);
            }

            try {
                bytes[i] = (byte) Integer.parseInt(items[i], 16);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("mac is invalid: " + mac, e);
            }
        }

        return bytes;
    }
}
