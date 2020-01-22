/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.bluetooth.le.beacon;

public class BluetoothAdvertisingData {

    private static final String PKT_BYTES_NUMBER = "1e";
    private static final String AD_BYTES_NUMBER = "02";
    private static final String AD_FLAG = "01";
    private static final String PAYLOAD_BYTES_NUMBER = "1a";
    private static final String MANUFACTURER_AD = "ff";
    private static final String BEACON_ID = "0215";

    private static final int MAJOR_MAX = 65535;
    private static final int MAJOR_MIN = 0;
    private static final int MINOR_MAX = 65535;
    private static final int MINOR_MIN = 0;
    private static final short TX_POWER_MAX = 126;
    private static final short TX_POWER_MIN = -127;
    private static final String UUID_DEFAULT = "aaaaaaaabbbbccccddddeeeeeeeeeeee";

    private BluetoothAdvertisingData() {

    }

    public static String getData(String uuid, Integer major, Integer minor, String companyCode, Integer txPower,
            boolean leLimited, boolean leGeneral, boolean brEDRSupported, boolean leBRController, boolean leBRHost) {

        String data = "";

        // Create flags
        String flags = "000";
        flags += Integer.toString(leBRHost ? 1 : 0);
        flags += Integer.toString(leBRController ? 1 : 0);
        flags += Integer.toString(brEDRSupported ? 1 : 0);
        flags += Integer.toString(leGeneral ? 1 : 0);
        flags += Integer.toString(leLimited ? 1 : 0);
        String flagsString = Integer.toHexString(Integer.parseInt(flags, 2));
        if (flagsString.length() == 1) {
            flagsString = "0" + flagsString;
        }

        txPower = inSetRange(txPower, TX_POWER_MAX, TX_POWER_MIN);

        String txPowerString = Integer.toHexString(txPower & 0xFF);
        if (txPowerString.length() == 1) {
            txPowerString = "0" + txPowerString;
        }

        // Create Advertising data
        data += PKT_BYTES_NUMBER;
        data += AD_BYTES_NUMBER;
        data += AD_FLAG;
        data += flagsString;
        data += PAYLOAD_BYTES_NUMBER;
        data += MANUFACTURER_AD;
        data += companyCode.substring(2, 4);
        data += companyCode.substring(0, 2);
        data += BEACON_ID;
        if (uuid.length() == 32) {
            data += inSetHex(uuid, UUID_DEFAULT);
        } else {
            data += UUID_DEFAULT;
        }
        data += to2BytesHex(inSetRange(major, MAJOR_MAX, MAJOR_MIN));
        data += to2BytesHex(inSetRange(minor, MINOR_MAX, MINOR_MIN));
        data += txPowerString;
        data += "00";

        return data;
    }

    private static String inSetHex(String uuid, String defaultUuid) {
        if (!uuid.matches("^[0-9a-fA-F]+$")) {
            return defaultUuid;
        } else {
            return uuid;
        }
    }

    private static int inSetRange(int value, int max, int min) {
        if (value <= max && value >= min) {
            return value;
        } else {
            return value > max ? max : min;
        }
    }

    public static String to2BytesHex(Integer in) {
        String out = Integer.toHexString(in);
        if (out.length() == 1) {
            out = "000" + out;
        } else if (out.length() == 2) {
            out = "00" + out;
        } else if (out.length() == 3) {
            out = "0" + out;
        } else if (out.length() > 4) {
            out = out.substring(out.length() - 4);
        }

        return out;
    }

}
