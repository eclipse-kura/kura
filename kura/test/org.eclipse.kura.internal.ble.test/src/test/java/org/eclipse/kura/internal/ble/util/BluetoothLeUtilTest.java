/*******************************************************************************
 * Copyright (c) 2021, 2026 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.internal.ble.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.eclipse.kura.bluetooth.le.beacon.AdvertisingReportEventType;
import org.eclipse.kura.bluetooth.le.beacon.AdvertisingReportRecord;
import org.junit.Test;

public class BluetoothLeUtilTest {

    @Test
    public void parseLEAdvertisementWithEventTypesTest() {

        byte[] record = { 0x3e, 0x00, 0x02, 0x05, // advertisement packet and subevent, 5 record
                0x00, 0x01, // Connectable Scannable Undirected advertising and random address
                1, 2, 3, 4, 5, 6, // address
                2, // data length - 2
                12, 10, // data
                0x01, 0x01, // Connectable Directed advertising and random address
                1, 2, 3, 4, 5, 6, // address
                2, // data length - 2
                12, 10, // data
                0x02, 0x01, // Scannable Undirected advertising Undirected advertising and random address
                1, 2, 3, 4, 5, 6, // address
                2, // data length - 2
                12, 10, // data
                0x03, 0x01, // Non-Connectable Non-Scannable Undirected advertising and random address
                1, 2, 3, 4, 5, 6, // address
                2, // data length - 2
                12, 10, // data
                0x04, 0x01, // Scan Response and random address
                1, 2, 3, 4, 5, 6, // address
                2, // data length - 2
                12, 10, // data
                123 // rssi
        };
        List<AdvertisingReportRecord> reports = BluetoothLeUtil.parseLEAdvertisement(record);
        assertEquals(5, reports.size());
        assertEquals(AdvertisingReportEventType.ADV_IND, reports.get(0).getEventType());
        assertEquals(AdvertisingReportEventType.ADV_DIRECT_IND, reports.get(1).getEventType());
        assertEquals(AdvertisingReportEventType.ADV_SCAN_IND, reports.get(2).getEventType());
        assertEquals(AdvertisingReportEventType.ADV_NONCONN_IND, reports.get(3).getEventType());
        assertEquals(AdvertisingReportEventType.SCAN_RSP, reports.get(4).getEventType());
    }

    @Test
    public void parseLEAdvertisementWithExtendedEventTypesTest() {

        byte[] record = { 0x3e, 0x00, 0x0D, 0x09, // advertisement packet and subevent, 6 record
                0x13, 0x10, // Extended advertising
                0x01, // random address
                1, 2, 3, 4, 5, 6, // address
                0, 0, 1, 99, // PHY None, PHY None, Sid, TxPower
                123, // rssi
                0x00, 0x00, // periodic advertising
                0x01, // random direct address
                1, 2, 3, 4, 5, 6, // address
                2, // data length - 2
                12, 10, // data
                0x15, 0x10, // Extended advertising
                0x01, // random address
                1, 2, 3, 4, 5, 6, // address
                0, 0, 1, 99, // PHY None, PHY None, Sid, TxPower
                123, // rssi
                0x00, 0x00, // periodic advertising
                0x01, // random direct address
                1, 2, 3, 4, 5, 6, // address
                2, // data length - 2
                12, 10, // data
                0x12, 0x10, // Extended advertising
                0x01, // random address
                1, 2, 3, 4, 5, 6, // address
                0, 0, 1, 99, // PHY None, PHY None, Sid, TxPower
                123, // rssi
                0x00, 0x00, // periodic advertising
                0x01, // random direct address
                1, 2, 3, 4, 5, 6, // address
                2, // data length - 2
                12, 10, // data
                0x10, 0x10, // Extended advertising
                0x01, // random address
                1, 2, 3, 4, 5, 6, // address
                0, 0, 1, 99, // PHY None, PHY None, Sid, TxPower
                123, // rssi
                0x00, 0x00, // periodic advertising
                0x01, // random direct address
                1, 2, 3, 4, 5, 6, // address
                2, // data length - 2
                12, 10, // data
                0x1B, 0x10, // Extended advertising
                0x01, // random address
                1, 2, 3, 4, 5, 6, // address
                0, 0, 1, 99, // PHY None, PHY None, Sid, TxPower
                123, // rssi
                0x00, 0x00, // periodic advertising
                0x01, // random direct address
                1, 2, 3, 4, 5, 6, // address
                2, // data length - 2
                12, 10, // data
                0x1A, 0x10, // Extended advertising
                0x01, // random address
                1, 2, 3, 4, 5, 6, // address
                0, 0, 1, 99, // PHY None, PHY None, Sid, TxPower
                123, // rssi
                0x00, 0x00, // periodic advertising
                0x01, // random direct address
                1, 2, 3, 4, 5, 6, // address
                2, // data length - 2
                12, 10, // data
                0x00, 0x00, // Extended advertising
                0x01, // random address
                1, 2, 3, 4, 5, 6, // address
                0, 0, 1, 99, // PHY None, PHY None, Sid, TxPower
                123, // rssi
                0x00, 0x00, // periodic advertising
                0x01, // random direct address
                1, 2, 3, 4, 5, 6, // address
                2, // data length - 2
                12, 10, // data
                0x20, 0x00, // Extended advertising
                0x01, // random address
                1, 2, 3, 4, 5, 6, // address
                0, 0, 1, 99, // PHY None, PHY None, Sid, TxPower
                123, // rssi
                0x00, 0x00, // periodic advertising
                0x01, // random direct address
                1, 2, 3, 4, 5, 6, // address
                2, // data length - 2
                12, 10, // data
                0x40, 0x00, // Extended advertising
                0x01, // random address
                1, 2, 3, 4, 5, 6, // address
                0, 0, 1, 99, // PHY None, PHY None, Sid, TxPower
                123, // rssi
                0x00, 0x00, // periodic advertising
                0x01, // random direct address
                1, 2, 3, 4, 5, 6, // address
                2, // data length - 2
                12, 10 // data
        };
        List<AdvertisingReportRecord> reports = BluetoothLeUtil.parseLEAdvertisement(record);
        assertEquals(9, reports.size());
        assertEquals(AdvertisingReportEventType.ADV_IND_EXT, reports.get(0).getEventType());
        assertEquals(AdvertisingReportEventType.ADV_DIRECT_IND_EXT, reports.get(1).getEventType());
        assertEquals(AdvertisingReportEventType.ADV_SCAN_IND_EXT, reports.get(2).getEventType());
        assertEquals(AdvertisingReportEventType.ADV_NONCONN_IND_EXT, reports.get(3).getEventType());
        assertEquals(AdvertisingReportEventType.SCAN_RSP_ADV_IND_EXT, reports.get(4).getEventType());
        assertEquals(AdvertisingReportEventType.SCAN_RSP_ADV_SCAN_IND_EXT, reports.get(5).getEventType());
        assertEquals(AdvertisingReportEventType.DATA_STATUS_COMPLETED, reports.get(6).getEventType());
        assertEquals(AdvertisingReportEventType.DATA_STATUS_INCOMPLETE_ONGOING, reports.get(7).getEventType());
        assertEquals(AdvertisingReportEventType.DATA_STATUS_INCOMPLETE_FINISHED, reports.get(8).getEventType());
    }

    // Data_Length is a single byte read as a signed value; 0x80-0xFF are legal
    // lengths
    // (Core Spec 5.2 7.7.65.13) but were previously sign-extended to a negative
    // int, causing
    // new byte[arrDataLength] to throw NegativeArraySizeException. 0x80 (128) must
    // decode
    // correctly instead.
    @Test
    public void parseLEAdvertisementWithDataLength0x80Test() {

        byte[] data = new byte[128];
        Arrays.fill(data, (byte) 0x11);

        byte[] head = { 0x3e, 0x00, 0x02, 0x01, // advertisement packet and subevent, 1 record
                0x00, 0x01, // Connectable Scannable Undirected advertising and random address
                1, 2, 3, 4, 5, 6, // address
                (byte) 0x80 // data length - 128
        };
        byte[] record = new byte[head.length + data.length + 1];
        System.arraycopy(head, 0, record, 0, head.length);
        System.arraycopy(data, 0, record, head.length, data.length);
        record[record.length - 1] = 123; // rssi

        List<AdvertisingReportRecord> reports = BluetoothLeUtil.parseLEAdvertisement(record);

        assertEquals(1, reports.size());
        assertEquals(128, reports.get(0).getLength());
        assertArrayEquals(data, reports.get(0).getReportData());
        assertEquals(123, reports.get(0).getRssi());
    }

    // A Data_Length that claims more bytes than are actually present in the buffer
    // must not
    // throw ArrayIndexOutOfBoundsException; the truncated report is dropped and
    // parsing stops.
    @Test
    public void parseLEAdvertisementWithTruncatedDataLengthTest() {

        byte[] record = { 0x3e, 0x00, 0x02, 0x01, // advertisement packet and subevent, 1 record
                0x00, 0x01, // Connectable Scannable Undirected advertising and random address
                1, 2, 3, 4, 5, 6, // address
                (byte) 0xF0 // data length - 240, but no data or rssi byte follows
        };

        List<AdvertisingReportRecord> reports = BluetoothLeUtil.parseLEAdvertisement(record);

        assertEquals(0, reports.size());
    }

    // A truncated report following valid ones must not discard the reports already
    // parsed.
    @Test
    public void parseLEAdvertisementWithTruncatedSecondRecordKeepsFirstTest() {

        byte[] record = { 0x3e, 0x00, 0x02, 0x02, // advertisement packet and subevent, 2 records
                0x00, 0x01, // record 1: Connectable Scannable Undirected advertising and random address
                1, 2, 3, 4, 5, 6, // address
                2, // data length - 2
                12, 10, // data
                0x01, 0x01, // record 2: Connectable Directed advertising and random address
                1, 2, 3, 4, 5, 6, // address
                (byte) 0xF0 // data length - 240, but no data or rssi byte follows
        };

        List<AdvertisingReportRecord> reports = BluetoothLeUtil.parseLEAdvertisement(record);

        assertEquals(1, reports.size());
        assertEquals(AdvertisingReportEventType.ADV_IND, reports.get(0).getEventType());
    }

    // Num_Reports is a single byte read as a signed value; 0x80-0xFF are legal counts but were
    // previously sign-extended to a negative int, making the for-loop never execute and silently
    // dropping every report in the advertisement. A count of 0x81 (129), with only one complete
    // report actually present, must still parse that one report instead of parsing none.
    @Test
    public void parseLEAdvertisementWithHighReportCountTest() {

        byte[] record = { 0x3e, 0x00, 0x02, (byte) 0x81, // advertisement packet and subevent, 129 records declared
                0x00, 0x01, // Connectable Scannable Undirected advertising and random address
                1, 2, 3, 4, 5, 6, // address
                2, // data length - 2
                12, 10, // data
                99 // rssi; no further reports actually present
        };

        List<AdvertisingReportRecord> reports = BluetoothLeUtil.parseLEAdvertisement(record);

        assertEquals(1, reports.size());
        assertEquals(99, reports.get(0).getRssi());
    }
}
