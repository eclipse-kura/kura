/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.ble.beacon;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.KuraBluetoothBeaconAdvertiserNotAvailable;
import org.eclipse.kura.KuraBluetoothCommandException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.bluetooth.le.BluetoothLeAdapter;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeacon;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconAdvertiser;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconDecoder;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconEncoder;
import org.eclipse.kura.bluetooth.le.beacon.BluetoothLeBeaconScanner;
import org.eclipse.kura.bluetooth.le.beacon.listener.BluetoothLeBeaconListener;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.internal.ble.util.BluetoothProcess;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluetoothLeBeaconManagerImplTest {

    public static final Logger logger = LoggerFactory.getLogger(BluetoothLeBeaconManagerImplTest.class);

    @Test
    public void testActivate() throws NoSuchFieldException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();
        ComponentContext ctxMock = mock(ComponentContext.class);

        svc.activate(ctxMock);

        assertNotNull(TestUtil.getFieldValue(svc, "listeners"));
    }

    @Test
    public void testDeactivate() {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();
        ComponentContext ctxMock = mock(ComponentContext.class);

        svc.deactivate(ctxMock);
    }

    @Test
    public void testAddDeleteBeaconScanner() throws NoSuchFieldException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();

        BluetoothLeAdapter adapter = mock(BluetoothLeAdapter.class);
        BluetoothLeBeaconDecoder<BluetoothLeBeacon> decoder = null;

        String interfaceName = "devName";
        when(adapter.getInterfaceName()).thenReturn(interfaceName);

        BluetoothLeBeaconScanner<BluetoothLeBeacon> scanner = svc.newBeaconScanner(adapter, decoder);

        assertNotNull(scanner);

        Map<String, List<BluetoothLeBeaconScannerImpl<BluetoothLeBeacon>>> scanners = (Map<String, List<BluetoothLeBeaconScannerImpl<BluetoothLeBeacon>>>) TestUtil
                .getFieldValue(svc, "scanners");
        assertNotNull(scanners);
        assertEquals(1, scanners.size());
        assertEquals(1, scanners.get(interfaceName).size());

        // add one more on the same device
        scanner = svc.newBeaconScanner(adapter, decoder);

        assertNotNull(scanner);

        assertEquals(1, scanners.size());
        assertEquals(2, scanners.get(interfaceName).size());

        // add one more on another device
        BluetoothLeAdapter adapter2 = mock(BluetoothLeAdapter.class);
        String interfaceName2 = "dev2Name";
        when(adapter2.getInterfaceName()).thenReturn(interfaceName2);

        BluetoothLeBeaconScanner<BluetoothLeBeacon> scanner2 = svc.newBeaconScanner(adapter2, decoder);

        assertNotNull(scanner2);

        assertEquals(2, scanners.size());
        assertEquals(1, scanners.get(interfaceName2).size());

        // delete one of the scanners
        svc.deleteBeaconScanner(scanner);
        assertEquals(2, scanners.size());
        assertEquals(1, scanners.get(interfaceName).size());

        // delete the (only) second interface scanner
        svc.deleteBeaconScanner(scanner2);
        assertEquals(2, scanners.size());
        assertEquals(0, scanners.get(interfaceName2).size());
    }

    @Test
    public void testAddDeleteBeaconAdvertizer() throws KuraBluetoothBeaconAdvertiserNotAvailable, NoSuchFieldException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();

        BluetoothLeAdapter adapter = mock(BluetoothLeAdapter.class);
        BluetoothLeBeaconEncoder<BluetoothLeBeacon> encoder = null;

        String interfaceName = "devName";
        when(adapter.getInterfaceName()).thenReturn(interfaceName);

        BluetoothLeBeaconAdvertiser<BluetoothLeBeacon> advertiser = svc.newBeaconAdvertiser(adapter, encoder);

        assertNotNull(advertiser);
        assertEquals(adapter, advertiser.getAdapter());

        Map<String, BluetoothLeBeaconAdvertiserImpl<BluetoothLeBeacon>> advertisers = (Map<String, BluetoothLeBeaconAdvertiserImpl<BluetoothLeBeacon>>) TestUtil
                .getFieldValue(svc, "advertisers");
        assertNotNull(advertisers);
        assertEquals(1, advertisers.size());

        // add one more on the same device - should fail
        try {
            svc.newBeaconAdvertiser(adapter, encoder);
            fail("Exception was expected");
        } catch (KuraBluetoothBeaconAdvertiserNotAvailable e) {
            // this exception is expected
        }

        // add one more on another device
        BluetoothLeAdapter adapter2 = mock(BluetoothLeAdapter.class);
        String interfaceName2 = "dev2Name";
        when(adapter2.getInterfaceName()).thenReturn(interfaceName2);

        advertiser = svc.newBeaconAdvertiser(adapter2, encoder);

        assertNotNull(advertiser);
        assertEquals(adapter2, advertiser.getAdapter());

        assertEquals(2, advertisers.size());

        // delete an advertiser
        svc.deleteBeaconAdvertiser(advertiser);

        assertEquals(1, advertisers.size());
    }

    @Test
    public void testStartBeaconAdvertising() throws KuraBluetoothCommandException {
        String interfaceName = "devName";

        AtomicBoolean visited = new AtomicBoolean(false);
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl() {

            @Override
            protected BluetoothProcess execHcitool(String intfName, String... cmd) throws IOException {
                assertEquals(interfaceName, intfName);
                assertArrayEquals(new String[] { "cmd", "0x08", "0x000a", "01" }, cmd);

                visited.set(true);

                return null;
            }
        };

        svc.startBeaconAdvertising(interfaceName);

        assertTrue(visited.get());
    }

    @Test(expected = KuraBluetoothCommandException.class)
    public void testFailToStartBeaconAdvertising() throws KuraBluetoothCommandException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl() {

            @Override
            protected BluetoothProcess execHcitool(String intfName, String... cmd) throws IOException {
                throw new IOException("test");
            }
        };

        svc.startBeaconAdvertising("devName");

    }

    @Test
    public void testStopBeaconAdvertising() throws KuraBluetoothCommandException {
        String interfaceName = "devName";

        AtomicBoolean visited = new AtomicBoolean(false);
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl() {

            @Override
            protected BluetoothProcess execHcitool(String intfName, String... cmd) throws IOException {
                assertEquals(interfaceName, intfName);
                assertArrayEquals(new String[] { "cmd", "0x08", "0x000a", "00" }, cmd);

                visited.set(true);

                return null;
            }
        };

        svc.stopBeaconAdvertising(interfaceName);

        assertTrue(visited.get());
    }

    @Test(expected = KuraBluetoothCommandException.class)
    public void testFailToStopBeaconAdvertising() throws KuraBluetoothCommandException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl() {

            @Override
            protected BluetoothProcess execHcitool(String intfName, String... cmd) throws IOException {
                throw new IOException("test");
            }
        };

        svc.stopBeaconAdvertising("devName");
    }

    @Test
    public void testUpdateBeaconAdvertisingInterval() throws KuraBluetoothCommandException {
        String interfaceName = "devName";

        AtomicBoolean visited = new AtomicBoolean(false);
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl() {

            @Override
            protected BluetoothProcess execHcitool(String intfName, String... cmd) throws IOException {
                assertEquals(interfaceName, intfName);
                assertArrayEquals(new String[] { "cmd", "0x08", "0x0006", "0E", "00", "0F", "00", "03", "00", "00",
                        "00", "00", "00", "00", "00", "00", "07", "00" }, cmd);

                visited.set(true);

                return null;
            }
        };

        svc.updateBeaconAdvertisingInterval(14, 15, interfaceName);

        assertTrue(visited.get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailToUpdateBeaconAdvertisingIntervalCheckMaxMin() throws KuraBluetoothCommandException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();

        svc.updateBeaconAdvertisingInterval(20, 15, "devName");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailToUpdateBeaconAdvertisingIntervalCheckMin() throws KuraBluetoothCommandException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();

        svc.updateBeaconAdvertisingInterval(1, 15, "devName");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailToUpdateBeaconAdvertisingIntervalCheckMin2() throws KuraBluetoothCommandException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();

        svc.updateBeaconAdvertisingInterval(65535, 66000, "devName");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailToUpdateBeaconAdvertisingIntervalCheckMax() throws KuraBluetoothCommandException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();

        svc.updateBeaconAdvertisingInterval(14, 65535, "devName");
    }

    @Test(expected = KuraBluetoothCommandException.class)
    public void testFailToUpdateBeaconAdvertisingInterval() throws KuraBluetoothCommandException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl() {

            @Override
            protected BluetoothProcess execHcitool(String intfName, String... cmd) throws IOException {
                throw new IOException("test");
            }
        };

        svc.updateBeaconAdvertisingInterval(14, 15, "devName");
    }

    @Test
    public void testUpdateBeaconAdvertisingData() throws KuraBluetoothCommandException {
        String interfaceName = "devName";
        AtomicBoolean visited = new AtomicBoolean(false);
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl() {

            @Override
            protected BluetoothProcess execHcitool(String intfName, String... cmd) throws IOException {
                assertEquals(interfaceName, intfName);
                assertArrayEquals(new String[] { "cmd", "0x08", "0x0008", "01", "02", "03", "04" }, cmd);

                visited.set(true);

                return null;
            }
        };

        BluetoothLeBeacon beacon = new BluetoothLeBeacon() {
        };
        BluetoothLeBeaconEncoder<BluetoothLeBeacon> encoder = mock(BluetoothLeBeaconEncoder.class);
        byte[] encodedBeacon = { 1, 2, 3, 4 };
        when(encoder.encode(beacon)).thenReturn(encodedBeacon);

        svc.updateBeaconAdvertisingData(beacon, encoder, interfaceName);

        assertTrue(visited.get());
    }

    @Test(expected = KuraBluetoothCommandException.class)
    public void testFailUpdateBeaconAdvertisingData() throws KuraBluetoothCommandException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl() {

            @Override
            protected BluetoothProcess execHcitool(String intfName, String... cmd) throws IOException {
                throw new IOException("test");
            }
        };

        BluetoothLeBeacon beacon = new BluetoothLeBeacon() {
        };
        BluetoothLeBeaconEncoder<BluetoothLeBeacon> encoder = mock(BluetoothLeBeaconEncoder.class);
        byte[] encodedBeacon = { 1, 2, 3, 4 };
        when(encoder.encode(beacon)).thenReturn(encodedBeacon);

        svc.updateBeaconAdvertisingData(beacon, encoder, "");
    }

    @Test(expected = KuraBluetoothCommandException.class)
    public void testProcessInputStreamSyntaxError() throws KuraException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();

        String str = "uNkNowN";
        svc.processInputStream(str);
    }

    @Test(expected = KuraBluetoothCommandException.class)
    public void testProcessInputStreamSyntaxError2() throws KuraException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();

        String str = "test\nusage:\nusg";
        svc.processInputStream(str);
    }

    @Test(expected = KuraBluetoothCommandException.class)
    public void testProcessInputStreamInvalid() throws KuraException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();

        String str = "iNvalid";
        svc.processInputStream(str);
    }

    @Test(expected = KuraBluetoothCommandException.class)
    public void testProcessInputStreamError() throws KuraException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();

        String str = "error";
        svc.processInputStream(str);
    }

    @Test
    public void testProcessInputStreamOK() throws KuraException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();

        String str = "< HCI Command: ogf 0x08, ocf 0x000a, plen 1\n  01 \n> HCI Event: 0x0e plen 4\n"
                + "  01 0A 20 00 \n";
        svc.processInputStream(str);
    }

    @Test
    public void testProcessInputStreamFail01() throws KuraException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();

        String str = "< HCI Command: ogf 0x08, ocf 0x000a, plen 1\n  01 \n> HCI Event: 0x0e plen 4\n"
                + "  01 0A 20 01 \n";
        try {
            svc.processInputStream(str);
            fail("Exception was expected.");
        } catch (KuraBluetoothCommandException e) {
            assertTrue(e.getMessage().contains("Unknown HCI Command (01)"));
        }
    }

    @Test
    public void testProcessInputStreamFail03() throws KuraException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();

        String str = "< HCI Command: ogf 0x08, ocf 0x000a, plen 1\n  01 \n> HCI Event: 0x0e plen 4\n"
                + "  01 0A 20 03 \n";
        try {
            svc.processInputStream(str);
            fail("Exception was expected.");
        } catch (KuraBluetoothCommandException e) {
            assertTrue(e.getMessage().contains("Hardware Failure (03)"));
        }
    }

    @Test
    public void testProcessInputStreamFail0c() throws KuraException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();

        String str = "< HCI Command: ogf 0x08, ocf 0x000a, plen 1\n  01 \n> HCI Event: 0x0e plen 4\n"
                + "  01 0A 20 0C \n";

        // no exception here!
        svc.processInputStream(str);

    }

    @Test
    public void testProcessInputStreamFail11() throws KuraException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();

        String str = "< HCI Command: ogf 0x08, ocf 0x000a, plen 1\n  01 \n> HCI Event: 0x0e plen 4\n"
                + "  01 0A 20 11 \n";
        try {
            svc.processInputStream(str);
            fail("Exception was expected.");
        } catch (KuraBluetoothCommandException e) {
            assertTrue(e.getMessage().contains("Unsupported Feature or Parameter Value (11)"));
        }
    }

    @Test
    public void testProcessInputStreamFail12() throws KuraException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();

        String str = "< HCI Command: ogf 0x08, ocf 0x000a, plen 1\n  01 \n> HCI Event: 0x0e plen 4\n"
                + "  01 0A 20 12 \n";
        try {
            svc.processInputStream(str);
            fail("Exception was expected.");
        } catch (KuraBluetoothCommandException e) {
            assertTrue(e.getMessage().contains("Invalid HCI Command Parameters (12)"));
        }
    }

    @Test
    public void testProcessInputStreamFail14() throws KuraException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();

        String str = "< HCI Command: ogf 0x08, ocf 0x000a, plen 1\n  01 \n> HCI Event: 0x0e plen 4\n"
                + "  01 0A 20 14 \n";
        try {
            svc.processInputStream(str);
            fail("Exception was expected.");
        } catch (KuraBluetoothCommandException e) {
            assertTrue(e.getMessage().contains("Error 14"));
        }
    }

    @Test
    public void testStartBeaconScanNoScanner() throws KuraBluetoothCommandException, NoSuchFieldException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl() {

            @Override
            protected BluetoothProcess execHcitool(String interfaceName, String... cmd) throws IOException {
                throw new IOException("test");
            }
        };

        svc.startBeaconScan("devNameNS");
    }

    @Test(expected = KuraBluetoothCommandException.class)
    public void testStartBeaconScanFail() throws KuraBluetoothCommandException, NoSuchFieldException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl() {

            @Override
            protected BluetoothProcess execHcitool(String interfaceName, String... cmd) throws IOException {
                throw new IOException("test");
            }
        };

        BluetoothLeAdapter adapter = mock(BluetoothLeAdapter.class);
        BluetoothLeBeaconDecoder<BluetoothLeBeacon> decoder = null;

        String interfaceName = "devName";
        when(adapter.getInterfaceName()).thenReturn(interfaceName);

        svc.newBeaconScanner(adapter, decoder);

        svc.startBeaconScan("devName");
    }

    @Test
    public void testStartStopBeaconScan() throws KuraBluetoothCommandException, NoSuchFieldException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl() {

            @Override
            protected BluetoothProcess execBtdump(String interfaceName) throws IOException {
                BluetoothProcess proc = mock(BluetoothProcess.class);
                when(proc.toString()).thenReturn(interfaceName);

                return proc;
            }

            @Override
            protected BluetoothProcess execHcitool(String interfaceName, String... cmd) throws IOException {
                BluetoothProcess proc = mock(BluetoothProcess.class);
                when(proc.toString()).thenReturn(interfaceName);

                return proc;
            }
        };

        BluetoothLeAdapter adapter = mock(BluetoothLeAdapter.class);
        BluetoothLeBeaconDecoder<BluetoothLeBeacon> decoder = null;

        String interfaceName = "devName";
        when(adapter.getInterfaceName()).thenReturn(interfaceName);

        BluetoothLeBeaconScanner<BluetoothLeBeacon> scanner = svc.newBeaconScanner(adapter, decoder);

        scanner.startBeaconScan(1);

        BluetoothProcess hProc = (BluetoothProcess) TestUtil.getFieldValue(svc, "hcitoolProc");
        BluetoothProcess dProc = (BluetoothProcess) TestUtil.getFieldValue(svc, "dumpProc");

        assertEquals(interfaceName, hProc.toString());
        assertEquals(interfaceName, dProc.toString());

        verify(hProc, times(1)).destroy();
        verify(dProc, times(1)).destroyBTSnoop();
    }

    @Test(expected = NullPointerException.class)
    public void testprocessBTSnoopRecordNullRecord() {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl();

        byte[] record = null;
        svc.processBTSnoopRecord(record);
    }

    @Test
    public void testprocessBTSnoopRecord() throws InterruptedException, NoSuchFieldException {
        BluetoothLeBeaconManagerImpl svc = new BluetoothLeBeaconManagerImpl() {

            @Override
            protected BluetoothProcess execBtdump(String interfaceName) throws IOException {
                BluetoothProcess proc = mock(BluetoothProcess.class);
                when(proc.toString()).thenReturn(interfaceName);

                return proc;
            }

            @Override
            protected BluetoothProcess execHcitool(String interfaceName, String... cmd) throws IOException {
                BluetoothProcess proc = mock(BluetoothProcess.class);
                when(proc.toString()).thenReturn(interfaceName);

                return proc;
            }
        };
        svc.activate(null);

        BluetoothLeAdapter adapter = mock(BluetoothLeAdapter.class);
        BluetoothLeBeaconDecoder<BluetoothLeBeacon> decoder = mock(BluetoothLeBeaconDecoder.class);

        BluetoothLeBeacon beacon = new BluetoothLeBeacon() {
        };
        when(decoder.decode(new byte[] { 12, 10 })).thenReturn(beacon); // record data

        String interfaceName = "devName";
        when(adapter.getInterfaceName()).thenReturn(interfaceName);

        when(decoder.getBeaconType()).thenReturn((Class<BluetoothLeBeacon>) beacon.getClass()); // for beacons stream
                                                                                                // filter

        AtomicBoolean visited = new AtomicBoolean(false);
        BluetoothLeBeaconListener<BluetoothLeBeacon> listener = new BluetoothLeBeaconListener<BluetoothLeBeacon>() {

            @Override
            public void onBeaconsReceived(BluetoothLeBeacon beacon) {
                logger.debug("visited");
                visited.set(true);
            }
        };

        Map<String, List<BluetoothLeBeaconScannerImpl<BluetoothLeBeacon>>> listeners = (Map<String, List<BluetoothLeBeaconScannerImpl<BluetoothLeBeacon>>>) TestUtil
                .getFieldValue(svc, "listeners");

        BluetoothLeBeaconScanner<BluetoothLeBeacon> scanner = svc.newBeaconScanner(adapter, decoder);
        scanner.addBeaconListener(listener);
        scanner.addBeaconListener(listener);

        // verify it was not added twice
        assertEquals(1, listeners.size());

        // snooping only works on running scanners
        new Thread(() -> {
            try {
                scanner.startBeaconScan(1);
            } catch (KuraBluetoothCommandException e) {
                // won't happen
            }
        }).start();

        Thread.sleep(20); // allow scanner to start

        byte[] record = { 0x4, 0x3e, 0x0, 0x02, 0x1, // advertisement packet and subevent, 1 record
                0x4, 0x1, // scan response and random address
                1, 2, 3, 4, 5, 6, // address
                2, // data length - 2
                12, 10, // data
                123 // rssi
        };
        svc.processBTSnoopRecord(record);

        assertTrue(visited.get());

        scanner.removeBeaconListener(listener);

        assertEquals(0, listeners.size());
    }
}
