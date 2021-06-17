package org.eclipse.kura.internal.ble.util;

import static org.junit.Assert.assertEquals;

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
}
