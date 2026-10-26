/*******************************************************************************
 * Copyright (c) 2017, 2026 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.internal.ble.ibeacon;

import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeacon;
import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeaconDecoder;
import org.eclipse.kura.util.base.TypeUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothLeIBeaconDecoderImpl implements BluetoothLeIBeaconDecoder {

    private static final Logger logger = LoggerFactory.getLogger(BluetoothLeIBeaconDecoderImpl.class);

    private static final byte[] BEACON_ID = { (byte) 0x02, (byte) 0x15 };
    private static final byte[] COMPANY_CODE = { (byte) 0x00, (byte) 0x4c };
    private static final byte[] IBEACON_PREFIX = { COMPANY_CODE[1], COMPANY_CODE[0], BEACON_ID[0], BEACON_ID[1] };
    // prefix + UUID + major + minor + txPower
    private static final int IBEACON_PAYLOAD_LENGTH = IBEACON_PREFIX.length + 21;
    private static final Pattern UUID_DASH_PATTERN = Pattern
            .compile("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)");

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

        if (logger.isDebugEnabled()) {
            logger.debug("Received packet: {}", TypeUtil.toHexString(b));
        }

        int ptr = 0;
        while (ptr < b.length) {

            if (logger.isDebugEnabled()) {
                logger.debug("Struct pointer: {}", ptr);
            }
            int structSize = b[ptr] & 0xFF;
            if (structSize == 0 || ptr + 1 >= b.length) {
                break;
            }

            byte dataType = b[ptr + 1];

            if (dataType == (byte) 0xFF // Data-Type: Manufacturer-Specific
                    && ptr + 2 + IBEACON_PAYLOAD_LENGTH <= b.length
                    && Arrays.equals(IBEACON_PREFIX, Arrays.copyOfRange(b, ptr + 2, ptr + 2 + IBEACON_PREFIX.length))) {
                return buildBeacon(b, ptr);
            }

            ptr += structSize + 1;
        }

        return null;
    }

    private static BluetoothLeIBeacon buildBeacon(byte[] b, int ptr) {
        BluetoothLeIBeacon beacon = new BluetoothLeIBeacon();

        if (ptr > 0) {
            beacon.setLeLimited((b[ptr - 1] & 0x01) == 0x01);
            beacon.setLeGeneral((b[ptr - 1] & 0x02) == 0x02);
            beacon.setBrEdrSupported((b[ptr - 1] & 0x04) == 0x04);
            beacon.setLeBrController((b[ptr - 1] & 0x08) == 0x08);
            beacon.setLeBrHost((b[ptr - 1] & 0x10) == 0x10);
        }

        int uuidPtr = ptr + 2 + IBEACON_PREFIX.length;
        int majorPtr = uuidPtr + 16;
        int minorPtr = uuidPtr + 18;

        StringBuilder uuid = new StringBuilder();
        for (byte ub : Arrays.copyOfRange(b, uuidPtr, majorPtr)) {
            uuid.append(String.format("%02X", ub));
        }
        beacon.setUuid(UUID.fromString(
                UUID_DASH_PATTERN.matcher(uuid.toString()).replaceFirst("$1-$2-$3-$4-$5")));

        int majorl = b[majorPtr + 1] & 0xFF;
        int majorh = b[majorPtr] & 0xFF;
        int minorl = b[minorPtr + 1] & 0xFF;
        int minorh = b[minorPtr] & 0xFF;
        beacon.setMajor((short) (majorh << 8 | majorl));
        beacon.setMinor((short) (minorh << 8 | minorl));
        beacon.setTxPower(b[minorPtr + 2]);
        return beacon;
    }

}
