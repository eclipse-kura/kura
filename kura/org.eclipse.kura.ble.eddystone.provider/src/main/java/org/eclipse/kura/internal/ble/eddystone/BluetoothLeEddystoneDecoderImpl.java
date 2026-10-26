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
package org.eclipse.kura.internal.ble.eddystone;

import java.util.Arrays;

import org.eclipse.kura.ble.eddystone.BluetoothLeEddystone;
import org.eclipse.kura.ble.eddystone.BluetoothLeEddystoneDecoder;
import org.eclipse.kura.util.base.TypeUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothLeEddystoneDecoderImpl implements BluetoothLeEddystoneDecoder {

    private static final Logger logger = LoggerFactory.getLogger(BluetoothLeEddystoneDecoderImpl.class);

    // See https://github.com/google/eddystone/blob/master/protocol-specification.md
    private static final byte UUID_LIST = (byte) 0x03;
    private static final byte[] EDDYSTONE_UUID = { (byte) 0xFE, (byte) 0xAA };
    private static final int EDDYSTONE_HEADER_LENGTH = 10;
    // header + namespace(10) + instance(6)
    private static final int EDDYSTONE_UID_LENGTH = EDDYSTONE_HEADER_LENGTH + 10 + 6;
    // header + URL scheme byte
    private static final int EDDYSTONE_URL_MIN_LENGTH = EDDYSTONE_HEADER_LENGTH + 1;

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

        if (logger.isDebugEnabled()) {
            logger.debug("Received packet: {}", TypeUtil.toHexString(b));
        }

        int ptr = 0;
        while (ptr < b.length) {

            if (logger.isDebugEnabled()) {
                logger.debug("Struct pointer: {}", ptr);
            }
            int structSize = b[ptr] & 0xFF;
            if (structSize == 0 || ptr + 3 >= b.length) {
                break;
            }

            if (b[ptr + 1] == UUID_LIST && b[ptr + 2] == EDDYSTONE_UUID[1] && b[ptr + 3] == EDDYSTONE_UUID[0]
                    && ptr + EDDYSTONE_HEADER_LENGTH <= b.length) {
                return buildEddystone(b, ptr);
            }

            ptr += structSize + 1;
        }

        return null;
    }

    private static BluetoothLeEddystone buildEddystone(byte[] b, int ptr) {
        EddystoneFrameType frameType = EddystoneFrameType.valueOf(b[ptr + 8]);
        if (frameType != EddystoneFrameType.UID && frameType != EddystoneFrameType.URL) {
            logger.warn("Eddystone frame type unknown or not supported {}", b[ptr + 8]);
            return null;
        }

        int minLength = frameType == EddystoneFrameType.UID ? EDDYSTONE_UID_LENGTH : EDDYSTONE_URL_MIN_LENGTH;
        if (ptr + minLength > b.length) {
            logger.warn("Eddystone frame truncated.");
            return null;
        }

        BluetoothLeEddystone eddystone = new BluetoothLeEddystone();
        short txPower = b[ptr + 9];

        if (frameType == EddystoneFrameType.UID) {
            byte[] namespace = Arrays.copyOfRange(b, ptr + 10, ptr + 20);
            byte[] instance = Arrays.copyOfRange(b, ptr + 20, ptr + 26);
            eddystone.configureEddystoneUIDFrame(namespace, instance, txPower);
        } else {
            byte[] urlHex = Arrays.copyOfRange(b, ptr + 11, b.length);
            eddystone.configureEddystoneURLFrame(
                    EddystoneURLScheme.decodeURLScheme(b[ptr + 10]) + EddystoneURLEncoding.decodeURL(urlHex),
                    txPower);
        }

        return eddystone;
    }

}
