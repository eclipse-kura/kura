/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.ble.ibeacon;

import java.util.Arrays;
import java.util.UUID;

import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeacon;
import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeaconDecoder;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothLeIBeaconDecoderImpl implements BluetoothLeIBeaconDecoder {

    private static final Logger logger = LoggerFactory.getLogger(BluetoothLeIBeaconDecoderImpl.class);

    private static final byte[] BEACON_ID = { (byte) 0x02, (byte) 0x15 };
    private static final byte[] COMPANY_CODE = { (byte) 0x00, (byte) 0x4c };
    private static final byte[] IBEACON_PREFIX = { COMPANY_CODE[1], COMPANY_CODE[0], BEACON_ID[0], BEACON_ID[1] };

    protected void activate(ComponentContext context) {
        logger.info("Activating Bluetooth Le IBeacon Codec...");
    }

    protected void deactivate(ComponentContext context) {
        logger.debug("Deactivating Bluetooth Le IBeacon Codec...");
    }

    @Override
    public Class<BluetoothLeIBeacon> getBeaconType() {
        return BluetoothLeIBeacon.class;
    }

    @Override
    public BluetoothLeIBeacon decode(byte[] reportData) {
        return parseEIRData(reportData);
    }

    /**
     * Parse EIR data from a BLE advertising report, extracting UUID, major and minor number.
     *
     * See Bluetooth Core 4.0; 8 EXTENDED INQUIRY RESPONSE DATA FORMAT
     *
     * @param b
     *            Array containing EIR data
     * @return BluetoothLeIBeacon or null if no beacon data present
     */
    private static BluetoothLeIBeacon parseEIRData(byte[] b) {

        int ptr = 0;
        while (ptr < b.length) {

            int structSize = b[ptr];
            if (structSize == 0) {
                break;
            }

            byte dataType = b[ptr + 1];

            if (dataType == (byte) 0xFF // Data-Type: Manufacturer-Specific
                    && Arrays.equals(IBEACON_PREFIX, Arrays.copyOfRange(b, ptr + 2, ptr + 2 + IBEACON_PREFIX.length))
                    && ptr > 0) {

                BluetoothLeIBeacon beacon = new BluetoothLeIBeacon();

                beacon.setLeLimited((b[ptr - 1] & 0x01) == 0x01);
                beacon.setLeGeneral((b[ptr - 1] & 0x02) == 0x02);
                beacon.setBrEdrSupported((b[ptr - 1] & 0x04) == 0x04);
                beacon.setLeBrController((b[ptr - 1] & 0x08) == 0x08);
                beacon.setLeBrHost((b[ptr - 1] & 0x10) == 0x10);

                int uuidPtr = ptr + 2 + IBEACON_PREFIX.length;
                int majorPtr = uuidPtr + 16;
                int minorPtr = uuidPtr + 18;

                StringBuilder uuid = new StringBuilder();
                for (byte ub : Arrays.copyOfRange(b, uuidPtr, majorPtr)) {
                    uuid.append(String.format("%02X", ub));
                }
                beacon.setUuid(UUID.fromString(uuid.toString().replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5")));

                int majorl = b[majorPtr + 1] & 0xFF;
                int majorh = b[majorPtr] & 0xFF;
                int minorl = b[minorPtr + 1] & 0xFF;
                int minorh = b[minorPtr] & 0xFF;
                beacon.setMajor((short) (majorh << 8 | majorl));
                beacon.setMinor((short) (minorh << 8 | minorl));
                beacon.setTxPower(b[minorPtr + 2]);
                return beacon;
            }

            ptr += structSize + 1;
        }

        return null;
    }

}
