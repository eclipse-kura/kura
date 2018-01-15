/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.ble.eddystone;

import java.util.Arrays;

import org.eclipse.kura.ble.eddystone.BluetoothLeEddystone;
import org.eclipse.kura.ble.eddystone.BluetoothLeEddystoneDecoder;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothLeEddystoneDecoderImpl implements BluetoothLeEddystoneDecoder {

    private static final Logger logger = LoggerFactory.getLogger(BluetoothLeEddystoneDecoderImpl.class);

    // See https://github.com/google/eddystone/blob/master/protocol-specification.md
    private static final byte UUID_LIST = (byte) 0x03;
    private static final byte[] EDDYSTONE_UUID = { (byte) 0xFE, (byte) 0xAA };

    protected void activate(ComponentContext context) {
        logger.info("Activating Bluetooth Le Eddystone Codec...");
    }

    protected void deactivate(ComponentContext context) {
        logger.debug("Deactivating Bluetooth Le Eddystone Codec...");
    }

    @Override
    public Class<BluetoothLeEddystone> getBeaconType() {
        return BluetoothLeEddystone.class;
    }

    @Override
    public BluetoothLeEddystone decode(byte[] reportData) {
        return parseEIRData(reportData);
    }

    /**
     * Parse EIR data from a BLE advertising report, extracting UID or URL.
     *
     * See Bluetooth Core 4.0; 8 EXTENDED INQUIRY RESPONSE DATA FORMAT
     *
     * @param b
     *            Array containing EIR data
     * @return BluetoothLeEddystone or null if no beacon data present
     */
    private static BluetoothLeEddystone parseEIRData(byte[] b) {

        int ptr = 0;
        while (ptr < b.length) {

            int structSize = b[ptr];
            if (structSize == 0) {
                break;
            }

            if (b[ptr + 1] == UUID_LIST && b[ptr + 2] == EDDYSTONE_UUID[1] && b[ptr + 3] == EDDYSTONE_UUID[0]) {

                BluetoothLeEddystone eddystone = new BluetoothLeEddystone();
                short txPower = b[ptr + 9];

                EddystoneFrameType frameType = EddystoneFrameType.valueOf(b[ptr + 8]);
                if (frameType.equals(EddystoneFrameType.UID)) {
                    byte[] namespace = Arrays.copyOfRange(b, ptr + 10, ptr + 20);
                    byte[] instance = Arrays.copyOfRange(b, ptr + 20, ptr + 26);
                    eddystone.configureEddystoneUIDFrame(namespace, instance, txPower);
                } else if (frameType.equals(EddystoneFrameType.URL)) {
                    byte[] urlHex = Arrays.copyOfRange(b, ptr + 11, b.length);
                    eddystone.configureEddystoneURLFrame(
                            EddystoneURLScheme.decodeURLScheme(b[ptr + 10]) + EddystoneURLEncoding.decodeURL(urlHex),
                            txPower);
                }

                return eddystone;
            }

            ptr += structSize + 1;
        }

        return null;
    }

}
