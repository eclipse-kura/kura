/*******************************************************************************
 * Copyright (c) 2017, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.ble.eddystone;

import org.eclipse.kura.ble.eddystone.BluetoothLeEddystone;
import org.eclipse.kura.ble.eddystone.BluetoothLeEddystoneEncoder;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothLeEddystoneEncoderImpl implements BluetoothLeEddystoneEncoder {

    private static final Logger logger = LoggerFactory.getLogger(BluetoothLeEddystoneEncoderImpl.class);

    // See https://github.com/google/eddystone/blob/master/protocol-specification.md
    private static final byte PKT_BYTES_NUMBER = (byte) 0x1f;
    private static final byte PAYLOAD_BYTES_NUMBER = (byte) 0x03;
    private static final byte UUID_LIST = (byte) 0x03;
    private static final byte[] EDDYSTONE_UUID = { (byte) 0xFE, (byte) 0xAA };
    private static final byte EDDYSTONE_UID_PAYLOAD_LENGTH = (byte) 0x17;
    private static final byte SERVICE_DATA = (byte) 0x16;
    private static final Integer URL_MAX_LENGTH = 17;
    private static final short TX_POWER_MAX = 126;
    private static final short TX_POWER_MIN = -127;

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
    public byte[] encode(BluetoothLeEddystone beacon) {
        byte flags = encodeFlags(beacon);

        // Create Advertising data
        EddystoneFrameType frameType = EddystoneFrameType.valueOf(beacon.getFrameType());
        if (frameType.equals(EddystoneFrameType.UID)) {
            return encodeUID(flags, beacon);
        } else if (frameType.equals(EddystoneFrameType.URL)) {
            return encodeURL(flags, beacon);
        } else if (frameType.equals(EddystoneFrameType.TLM)) {
            return encodeTLM(flags, beacon);
        } else if (frameType.equals(EddystoneFrameType.EID)) {
            return encodeEID(flags, beacon);
        } else {
            return new byte[1];
        }
    }

    private byte encodeFlags(BluetoothLeEddystone beacon) {
        // Create flags
        byte flags = 0x00;
        flags |= beacon.isLeLimited() ? 0x01 : 0x00;
        flags |= beacon.isLeGeneral() ? 0x02 : 0x00;
        flags |= beacon.isBrEdrSupported() ? 0x04 : 0x00;
        flags |= beacon.isLeBrController() ? 0x08 : 0x00;
        flags |= beacon.isLeBrHost() ? 0x10 : 0x00;
        return flags;
    }

    private byte[] encodeUID(byte flags, BluetoothLeEddystone beacon) {
        byte[] data = new byte[PKT_BYTES_NUMBER + 1];

        data[0] = PKT_BYTES_NUMBER;
        data[1] = BluetoothLeBeacon.AD_BYTES_NUMBER;
        data[2] = BluetoothLeBeacon.AD_FLAG;
        data[3] = flags;
        data[4] = PAYLOAD_BYTES_NUMBER;
        data[5] = UUID_LIST;
        data[6] = EDDYSTONE_UUID[1];
        data[7] = EDDYSTONE_UUID[0];
        data[8] = EDDYSTONE_UID_PAYLOAD_LENGTH;
        data[9] = SERVICE_DATA;
        data[10] = EDDYSTONE_UUID[1];
        data[11] = EDDYSTONE_UUID[0];
        data[12] = EddystoneFrameType.UID.getFrameTypeCode();
        data[13] = (byte) (setInRange(beacon.getTxPower(), TX_POWER_MAX, TX_POWER_MIN) & 0xff);
        System.arraycopy(beacon.getNamespace(), 0, data, 14, 10);
        System.arraycopy(beacon.getInstance(), 0, data, 24, 6);
        data[30] = (byte) 0x00;
        data[31] = (byte) 0x00;

        return data;
    }

    private byte[] encodeURL(byte flags, BluetoothLeEddystone beacon) {
        byte[] hexUrl = EddystoneURLEncoding.encodeURL(beacon.getUrl());
        byte[] data = new byte[15 + URL_MAX_LENGTH];

        if (hexUrl.length <= URL_MAX_LENGTH) {
            data[0] = (byte) (14 + hexUrl.length);
            data[1] = BluetoothLeBeacon.AD_BYTES_NUMBER;
            data[2] = BluetoothLeBeacon.AD_FLAG;
            data[3] = flags;
            data[4] = PAYLOAD_BYTES_NUMBER;
            data[5] = UUID_LIST;
            data[6] = EDDYSTONE_UUID[1];
            data[7] = EDDYSTONE_UUID[0];
            data[8] = (byte) (6 + hexUrl.length);
            data[9] = SERVICE_DATA;
            data[10] = EDDYSTONE_UUID[1];
            data[11] = EDDYSTONE_UUID[0];
            data[12] = EddystoneFrameType.URL.getFrameTypeCode();
            data[13] = (byte) (setInRange(beacon.getTxPower(), TX_POWER_MAX, TX_POWER_MIN) & 0xff);
            data[14] = EddystoneURLScheme.encodeURLScheme(beacon.getUrlScheme()).getUrlSchemeCode();
            System.arraycopy(hexUrl, 0, data, 15, hexUrl.length);
            for (int i = hexUrl.length; i < URL_MAX_LENGTH; i++) {
                data[15 + i] = (byte) 0x00;
            }
        } else {
            logger.warn("Invalid Eddystone URL frame or url too long.");
        }
        return data;
    }

    private byte[] encodeTLM(byte flags, BluetoothLeEddystone beacon) {
        // Not implemented yet
        return new byte[1];
    }

    private byte[] encodeEID(byte flags, BluetoothLeEddystone beacon) {
        // Not implemented yet
        return new byte[1];
    }

    private short setInRange(short value, short max, short min) {
        if (value <= max && value >= min) {
            return value;
        } else {
            return (value > max) ? max : min;
        }
    }
}
