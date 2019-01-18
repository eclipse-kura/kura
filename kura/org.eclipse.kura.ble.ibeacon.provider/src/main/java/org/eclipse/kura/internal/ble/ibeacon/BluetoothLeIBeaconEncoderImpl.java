/*******************************************************************************
 * Copyright (c) 2017, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.ble.ibeacon;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeacon;
import org.eclipse.kura.ble.ibeacon.BluetoothLeIBeaconEncoder;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothLeIBeaconEncoderImpl implements BluetoothLeIBeaconEncoder {

    private static final Logger logger = LoggerFactory.getLogger(BluetoothLeIBeaconEncoderImpl.class);

    private static final byte PKT_BYTES_NUMBER = (byte) 0x1e;
    private static final byte PAYLOAD_BYTES_NUMBER = (byte) 0x1a;
    private static final byte MANUFACTURER_AD = (byte) 0xff;
    private static final byte[] BEACON_ID = { (byte) 0x02, (byte) 0x15 };
    private static final byte[] COMPANY_CODE = { (byte) 0x00, (byte) 0x4c };
    private static final short TX_POWER_MAX = 126;
    private static final short TX_POWER_MIN = -127;

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
    public byte[] encode(BluetoothLeIBeacon beacon) {
        byte[] data = new byte[PKT_BYTES_NUMBER + 2];

        byte flags = encodeFlags(beacon);

        // Create Advertising data
        data[0] = PKT_BYTES_NUMBER;
        data[1] = BluetoothLeBeacon.AD_BYTES_NUMBER;
        data[2] = BluetoothLeBeacon.AD_FLAG;
        data[3] = flags;
        data[4] = PAYLOAD_BYTES_NUMBER;
        data[5] = MANUFACTURER_AD;
        data[6] = COMPANY_CODE[1];
        data[7] = COMPANY_CODE[0];
        data[8] = BEACON_ID[0];
        data[9] = BEACON_ID[1];
        System.arraycopy(getBytesFromUUID(beacon.getUuid()), 0, data, 10, 16);
        data[26] = (byte) (beacon.getMajor() >> 8 & 0xff);
        data[27] = (byte) (beacon.getMajor() & 0xff);
        data[28] = (byte) (beacon.getMinor() >> 8 & 0xff);
        data[29] = (byte) (beacon.getMinor() & 0xff);
        data[30] = (byte) (setInRange(beacon.getTxPower(), TX_POWER_MAX, TX_POWER_MIN) & 0xff);
        data[31] = 0x00;

        return data;
    }

    private byte encodeFlags(BluetoothLeIBeacon beacon) {
        // Create flags
        byte flags = 0x00;
        flags |= beacon.isLeLimited() ? 0x01 : 0x00;
        flags |= beacon.isLeGeneral() ? 0x02 : 0x00;
        flags |= beacon.isBrEdrSupported() ? 0x04 : 0x00;
        flags |= beacon.isLeBrController() ? 0x08 : 0x00;
        flags |= beacon.isLeBrHost() ? 0x10 : 0x00;
        return flags;
    }

    private static byte[] getBytesFromUUID(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return byteBuffer.array();
    }

    private short setInRange(short value, short max, short min) {
        if (value <= max && value >= min) {
            return value;
        } else {
            return (value > max) ? max : min;
        }
    }
}
