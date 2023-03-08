/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.nm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.ExitStatus;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.NetworkInterfaceType;
import org.eclipse.kura.usb.UsbNetDevice;
import org.freedesktop.NetworkManager;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.networkmanager.Device;
import org.freedesktop.networkmanager.GetAppliedConnectionTuple;
import org.freedesktop.networkmanager.Settings;
import org.freedesktop.networkmanager.device.Generic;
import org.freedesktop.networkmanager.device.Wireless;
import org.freedesktop.networkmanager.settings.Connection;
import org.junit.After;
import org.junit.Test;

public class NMDbusConnectorTest {

    DBusConnection dbusConnection = mock(DBusConnection.class, RETURNS_SMART_NULLS);
    NetworkManager mockedNetworkManager = mock(NetworkManager.class);
    NMDbusConnector instanceNMDbusConnector;
    DBusConnection dbusConnectionInternal;
    CommandExecutorService commandExecutorService = mock(CommandExecutorService.class);

    Boolean hasDBusExceptionBeenThrown = false;
    Boolean hasNoSuchElementExceptionThrown = false;
    Boolean hasNullPointerExceptionThrown = false;
    Boolean hasKuraExceptionThrown = false;

    NetworkInterfaceStatus netInterface;
    NetworkService networkService;

    Map<String, Device> mockDevices = new HashMap<>();
    Connection mockConnection;

    List<String> internalStringList;
    Map<String, Object> netConfig = new HashMap<>();

    private static String iwRegGetOutput = "global\n" + "country CA: DFS-FCC\n"
            + "    (2402 - 2472 @ 40), (N/A, 30), (N/A)\n"
            + " (5150 - 5250 @ 80), (N/A, 23), (N/A), NO-OUTDOOR, AUTO-BW\n"
            + " (5250 - 5350 @ 80), (N/A, 24), (0 ms), DFS, AUTO-BW\n" + " (5470 - 5600 @ 80), (N/A, 24), (0 ms), DFS\n"
            + " (5650 - 5730 @ 80), (N/A, 24), (0 ms), DFS\n" + "   (5735 - 5835 @ 80), (N/A, 30), (N/A)\n" + "\n"
            + "phy#0\n" + "country 99: DFS-UNSET\n" + "  (2402 - 2482 @ 40), (6, 20), (N/A)\n"
            + " (2474 - 2494 @ 20), (6, 20), (N/A)\n" + "   (5140 - 5360 @ 160), (6, 20), (N/A)\n"
            + " (5460 - 5860 @ 160), (6, 20), (N/A)";

    private static String iwWlan0InfoOutput = "Interface wlan0\n" + "  ifindex 3\n" + "    wdev 0x1\n"
            + " addr dc:a6:32:a6:e0:c2\n" + "   ssid kura_gateway_raspberry_pi\n" + "   type AP\n" + " wiphy 0\n"
            + "  channel 1 (2412 MHz), width: 20 MHz, center1: 2412 MHz\n" + " txpower 31.00 dBm";

    private static String iwPhyPhy0InfoOutput = "Wiphy phy0\n" + " wiphy index: 0\n" + "   max # scan SSIDs: 10\n"
            + " max scan IEs length: 2048 bytes\n" + "  max # sched scan SSIDs: 16\n" + " max # match sets: 16\n"
            + " Retry short limit: 7\n" + " Retry long limit: 4\n" + " Coverage class: 0 (up to 0m)\n"
            + " Device supports roaming.\n" + " Device supports T-DLS.\n" + " Supported Ciphers:\n"
            + "       * WEP40 (00-0f-ac:1)\n" + "     * WEP104 (00-0f-ac:5)\n" + "     * TKIP (00-0f-ac:2)\n"
            + "      * CCMP-128 (00-0f-ac:4)\n" + "      * CMAC (00-0f-ac:6)\n" + " Available Antennas: TX 0 RX 0\n"
            + "    Supported interface modes:\n" + "        * IBSS\n" + "      * managed\n" + "        * AP\n"
            + "         * P2P-client\n" + "         * P2P-GO\n" + "      * P2P-device\n" + "    Band 1:\n"
            + "      Capabilities: 0x1062\n" + "         HT20/HT40\n" + "         Static SM Power Save\n"
            + "         RX HT20 SGI\n" + "          RX HT40 SGI\n" + "         No RX STBC\n"
            + "           Max AMSDU length: 3839 bytes\n" + "         DSSS/CCK HT40\n"
            + "     Maximum RX AMPDU length 65535 bytes (exponent: 0x003)\n"
            + "     Minimum RX AMPDU time spacing: 16 usec (0x07)\n" + "     HT TX/RX MCS rate indexes supported: 0-7\n"
            + "     Bitrates (non-HT):\n" + "         * 1.0 Mbps\n"
            + "           * 2.0 Mbps (short preamble supported)\n" + "         * 5.5 Mbps (short preamble supported)\n"
            + "         * 11.0 Mbps (short preamble supported)\n" + "           * 6.0 Mbps\n" + "         * 9.0 Mbps\n"
            + "           * 12.0 Mbps\n" + "          * 18.0 Mbps\n" + "         * 24.0 Mbps\n"
            + "          * 36.0 Mbps\n" + "          * 48.0 Mbps\n" + "         * 54.0 Mbps\n" + "      Frequencies:\n"
            + "         * 2412 MHz [1] (20.0 dBm)\n" + "         * 2417 MHz [2] (20.0 dBm)\n"
            + "            * 2422 MHz [3] (20.0 dBm)\n" + "         * 2427 MHz [4] (20.0 dBm)\n"
            + "            * 2432 MHz [5] (20.0 dBm)\n" + "         * 2437 MHz [6] (20.0 dBm)\n"
            + "            * 2442 MHz [7] (20.0 dBm)\n" + "         * 2447 MHz [8] (20.0 dBm)\n"
            + "            * 2452 MHz [9] (20.0 dBm)\n" + "         * 2457 MHz [10] (20.0 dBm)\n"
            + "           * 2462 MHz [11] (20.0 dBm)\n" + "         * 2467 MHz [12] (disabled)\n"
            + "           * 2472 MHz [13] (disabled)\n" + "         * 2484 MHz [14] (disabled)\n" + "   Band 2:\n"
            + "      Capabilities: 0x1062\n" + "         HT20/HT40\n" + "            Static SM Power Save\n"
            + "         RX HT20 SGI\n" + "         RX HT40 SGI\n" + "          No RX STBC\n"
            + "           Max AMSDU length: 3839 bytes\n" + "         DSSS/CCK HT40\n"
            + "        Maximum RX AMPDU length 65535 bytes (exponent: 0x003)\n"
            + "     Minimum RX AMPDU time spacing: 16 usec (0x07)\n" + "     HT TX/RX MCS rate indexes supported: 0-7\n"
            + "     VHT Capabilities (0x00001020):\n" + "         Max MPDU length: 3895\n"
            + "         Supported Channel Width: neither 160 nor 80+80\n" + "           short GI (80 MHz)\n"
            + "         SU Beamformee\n" + "        VHT RX MCS set:\n" + "          1 streams: MCS 0-9\n"
            + "         2 streams: not supported\n" + "         3 streams: not supported\n"
            + "         4 streams: not supported\n" + "         5 streams: not supported\n"
            + "         6 streams: not supported\n" + "         7 streams: not supported\n"
            + "         8 streams: not supported\n" + "     VHT RX highest supported: 0 Mbps\n"
            + "     VHT TX MCS set:\n" + "          1 streams: MCS 0-9\n" + "         2 streams: not supported\n"
            + "         3 streams: not supported\n" + "         4 streams: not supported\n"
            + "         5 streams: not supported\n" + "         6 streams: not supported\n"
            + "         7 streams: not supported\n" + "         8 streams: not supported\n"
            + "     VHT TX highest supported: 0 Mbps\n" + "     Bitrates (non-HT):\n" + "           * 6.0 Mbps\n"
            + "           * 9.0 Mbps\n" + "         * 12.0 Mbps\n" + "          * 18.0 Mbps\n"
            + "          * 24.0 Mbps\n" + "         * 36.0 Mbps\n" + "          * 48.0 Mbps\n"
            + "          * 54.0 Mbps\n" + "     Frequencies:\n" + "         * 5170 MHz [34] (disabled)\n"
            + "         * 5180 MHz [36] (20.0 dBm)\n" + "           * 5190 MHz [38] (disabled)\n"
            + "         * 5200 MHz [40] (20.0 dBm)\n" + "           * 5210 MHz [42] (disabled)\n"
            + "         * 5220 MHz [44] (20.0 dBm)\n" + "           * 5230 MHz [46] (disabled)\n"
            + "         * 5240 MHz [48] (20.0 dBm)\n" + "         * 5260 MHz [52] (20.0 dBm) (no IR, radar detection)\n"
            + "         * 5280 MHz [56] (20.0 dBm) (no IR, radar detection)\n"
            + "         * 5300 MHz [60] (20.0 dBm) (no IR, radar detection)\n"
            + "         * 5320 MHz [64] (20.0 dBm) (no IR, radar detection)\n"
            + "         * 5500 MHz [100] (20.0 dBm) (no IR, radar detection)\n"
            + "         * 5520 MHz [104] (20.0 dBm) (no IR, radar detection)\n"
            + "         * 5540 MHz [108] (20.0 dBm) (no IR, radar detection)\n"
            + "         * 5560 MHz [112] (20.0 dBm) (no IR, radar detection)\n"
            + "         * 5580 MHz [116] (20.0 dBm) (no IR, radar detection)\n"
            + "         * 5600 MHz [120] (disabled)\n" + "          * 5620 MHz [124] (disabled)\n"
            + "         * 5640 MHz [128] (disabled)\n"
            + "         * 5660 MHz [132] (20.0 dBm) (no IR, radar detection)\n"
            + "         * 5680 MHz [136] (20.0 dBm) (no IR, radar detection)\n"
            + "         * 5700 MHz [140] (20.0 dBm) (no IR, radar detection)\n"
            + "         * 5720 MHz [144] (20.0 dBm) (no IR, radar detection)\n"
            + "         * 5745 MHz [149] (20.0 dBm)\n" + "          * 5765 MHz [153] (20.0 dBm)\n"
            + "         * 5785 MHz [157] (20.0 dBm)\n" + "          * 5805 MHz [161] (20.0 dBm)\n"
            + "         * 5825 MHz [165] (20.0 dBm)\n" + "  Supported commands:\n" + "       * new_interface\n"
            + "      * set_interface\n" + "      * new_key\n" + "        * start_ap\n" + "       * join_ibss\n"
            + "      * set_pmksa\n" + "      * del_pmksa\n" + "      * flush_pmksa\n" + "      * remain_on_channel\n"
            + "      * frame\n" + "      * set_wiphy_netns\n" + "      * set_channel\n" + "        * tdls_oper\n"
            + "      * start_sched_scan\n" + "      * start_p2p_device\n" + "       * connect\n"
            + "        * disconnect\n" + "      * crit_protocol_start\n" + "        * crit_protocol_stop\n"
            + "      * update_connect_params\n" + " software interface modes (can always be added):\n"
            + " valid interface combinations:\n"
            + "      * #{ managed } <= 1, #{ P2P-device } <= 1, #{ P2P-client, P2P-GO } <= 1,\n"
            + "        total <= 3, #channels <= 2\n"
            + "      * #{ managed } <= 1, #{ AP } <= 1, #{ P2P-client } <= 1, #{ P2P-device } <= 1,\n"
            + "        total <= 4, #channels <= 1\n" + "    Device supports scan flush.\n"
            + " Device supports randomizing MAC-addr in sched scans.\n" + " max # scan plans: 1\n"
            + " max scan plan interval: 508\n" + "  max scan plan iterations: 0\n" + " Supported TX frame types:\n"
            + "      * managed: 0x00 0x10 0x20 0x30 0x40 0x50 0x60 0x70 0x80 0x90 0xa0 0xb0 0xc0 0xd0 0xe0 0xf0\n"
            + "      * AP: 0x00 0x10 0x20 0x30 0x40 0x50 0x60 0x70 0x80 0x90 0xa0 0xb0 0xc0 0xd0 0xe0 0xf0\n"
            + "      * P2P-client: 0x00 0x10 0x20 0x30 0x40 0x50 0x60 0x70 0x80 0x90 0xa0 0xb0 0xc0 0xd0 0xe0 0xf0\n"
            + "      * P2P-GO: 0x00 0x10 0x20 0x30 0x40 0x50 0x60 0x70 0x80 0x90 0xa0 0xb0 0xc0 0xd0 0xe0 0xf0\n"
            + "      * P2P-device: 0x00 0x10 0x20 0x30 0x40 0x50 0x60 0x70 0x80 0x90 0xa0 0xb0 0xc0 0xd0 0xe0 0xf0\n"
            + " Supported RX frame types:\n" + "         * managed: 0x40 0xd0\n"
            + "      * AP: 0x00 0x20 0x40 0xa0 0xb0 0xc0 0xd0\n" + "         * P2P-client: 0x40 0xd0\n"
            + "      * P2P-GO: 0x00 0x20 0x40 0xa0 0xb0 0xc0 0xd0\n" + "         * P2P-device: 0x40 0xd0\n"
            + " Supported extended features:\n" + "     * [ CQM_RSSI_LIST ]: multiple CQM_RSSI_THOLD records\n"
            + "     * [ 4WAY_HANDSHAKE_STA_PSK ]: 4-way handshake with PSK in station mode\n"
            + "     * [ 4WAY_HANDSHAKE_STA_1X ]: 4-way handshake with 802.1X in station mode\n"
            + "     * [ DFS_OFFLOAD ]: DFS offload\n" + "     * [ 4WAY_HANDSHAKE_AP_PSK ]: AP mode PSK offload support";

    @After
    public void tearDown() {
        resetSingleton(NMDbusConnector.class, "instance");
    }

    public static <T> void resetSingleton(Class<T> clazz, String fieldName) {
        Field instance;
        try {
            instance = clazz.getDeclaredField(fieldName);
            instance.setAccessible(true);
            instance.set(null, null);
            instance.setAccessible(false);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    @Test
    public void getDbusConnectionShouldWork() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        whenGetDbusConnectionIsRun();

        thenNoExceptionIsThrown();

        thenGetDbusConnectionReturns(this.dbusConnectionInternal);
    }

    @Test
    public void checkPermissionsShouldWork() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedPermissions();

        whenCheckPermissionsIsRun();

        thenNoExceptionIsThrown();
        thenCheckPermissionsRan();
    }

    @Test
    public void checkVersionShouldWork() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedVersion();

        whenCheckVersionIsRun();

        thenNoExceptionIsThrown();
        thenCheckVersionIsRun();
    }

    @Test
    public void getInterfacesShouldWork() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED,
                true);
        givenMockedDevice("wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true);
        givenMockedDeviceList();

        whenGetInterfacesIsCalled();

        thenNoExceptionIsThrown();
        thenGetInterfacesReturn(Arrays.asList("wlan0", "eth0"));
    }

    @Test
    public void applyShouldDoNothingWithNoCache() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED,
                true);
        givenMockedDevice("wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true);
        givenMockedDeviceList();

        whenApplyIsCalled();

        thenNoExceptionIsThrown();
        thenNetworkSettingsDidNotChangeForDevice("eth0");
        thenNetworkSettingsDidNotChangeForDevice("wlan0");
    }

    @Test
    public void applyShouldThrowWithNullMap() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED,
                true);
        givenMockedDevice("wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true);
        givenMockedDeviceList();

        whenApplyIsCalledWith(null);

        thenNullPointerExceptionIsThrown();
    }

    @Test
    public void applyShouldThrowWithEmptyMap() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED,
                true);
        givenMockedDevice("wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true);
        givenMockedDeviceList();

        whenApplyIsCalledWith(new HashMap<String, Object>());

        thenNoSuchElementExceptionIsThrown();
    }

    @Test
    public void applyShouldDoNothingWithEnabledUnsupportedDevices() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("unused0", NMDeviceType.NM_DEVICE_TYPE_UNUSED1, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED,
                true);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "unused0");
        givenNetworkConfigMapWith("net.interface.unused0.config.dhcpClient4.enabled", true);
        givenNetworkConfigMapWith("net.interface.unused0.config.ip4.status", "netIPv4StatusEnabledWAN");

        whenApplyIsCalledWith(this.netConfig);

        thenNoExceptionIsThrown();
        thenNetworkSettingsDidNotChangeForDevice("unused0");
    }

    @Test
    public void applyShouldWorkWithEnabledEthernet() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED,
                true);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "eth0");
        givenNetworkConfigMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");

        whenApplyIsCalledWith(this.netConfig);

        thenNoExceptionIsThrown();
        thenConnectionUpdateIsCalledFor("eth0");
        thenActivateConnectionIsCalledFor("eth0");
    }

    @Test
    public void applyShouldWorkWithEnabledEthernetWithoutInitialConnection() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED,
                false);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "eth0");
        givenNetworkConfigMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");

        whenApplyIsCalledWith(this.netConfig);

        thenNoExceptionIsThrown();
        thenAddAndActivateConnectionIsCalledFor("eth0");
    }

    @Test
    public void applyShouldWorkWithDisabledEthernet() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_ACTIVATED, true);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "eth,");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusDisabled");

        whenApplyIsCalledWith(this.netConfig);

        thenNoExceptionIsThrown();
        thenDisconnectIsCalledFor("eth0");
    }

    @Test
    public void applyShouldNotDisableLoopbackDevice() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("lo", NMDeviceType.NM_DEVICE_TYPE_LOOPBACK, NMDeviceState.NM_DEVICE_STATE_ACTIVATED, true);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "lo,");
        givenNetworkConfigMapWith("net.interface.lo.config.ip4.status", "netIPv4StatusDisabled");

        whenApplyIsCalledWith(this.netConfig);

        thenNoExceptionIsThrown();
        thenNetworkSettingsDidNotChangeForDevice("lo");
    }

    @Test
    public void applyShouldNotDisableLoopbackDeviceOldVersionOfNM() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("lo", NMDeviceType.NM_DEVICE_TYPE_GENERIC, NMDeviceState.NM_DEVICE_STATE_ACTIVATED, true);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "lo,");
        givenNetworkConfigMapWith("net.interface.lo.config.ip4.status", "netIPv4StatusDisabled");

        whenApplyIsCalledWith(this.netConfig);

        thenNoExceptionIsThrown();
        thenNetworkSettingsDidNotChangeForDevice("lo");
    }

    @Test
    public void getInterfaceStatusShouldWorkEthernet() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_ACTIVATED, true);
        givenMockedDeviceList();
        givenNetworkServiceThatAlwaysReturnsEmpty();

        whenGetInterfaceStatus("eth0", this.networkService, this.commandExecutorService);

        thenNoExceptionIsThrown();
        thenInterfaceStatusIsNotNull();
        thenNetInterfaceTypeIs(NetworkInterfaceType.ETHERNET);
    }

    @Test
    public void getInterfaceStatusShouldWorkLoopback() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("lo", NMDeviceType.NM_DEVICE_TYPE_LOOPBACK, NMDeviceState.NM_DEVICE_STATE_FAILED, true);
        givenMockedDeviceList();
        givenNetworkServiceThatAlwaysReturnsEmpty();

        whenGetInterfaceStatus("lo", this.networkService, this.commandExecutorService);

        thenNoExceptionIsThrown();
        thenInterfaceStatusIsNotNull();
        thenNetInterfaceTypeIs(NetworkInterfaceType.LOOPBACK);
    }

    @Test
    public void getInterfaceStatusShouldWorkUnsuported() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("unused0", NMDeviceType.NM_DEVICE_TYPE_UNUSED1, NMDeviceState.NM_DEVICE_STATE_FAILED, true);
        givenMockedDeviceList();
        givenNetworkServiceThatAlwaysReturnsEmpty();

        whenGetInterfaceStatus("unused0", this.networkService, this.commandExecutorService);

        thenNoExceptionIsThrown();
        thenInterfaceStatusIsNull();
    }

    @Test
    public void getInterfaceStatusShouldWorkWireless() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI, NMDeviceState.NM_DEVICE_STATE_FAILED, true);
        givenMockedDeviceList();
        givenNetworkServiceThatAlwaysReturnsEmpty();

        whenGetInterfaceStatus("wlan0", this.networkService, this.commandExecutorService);

        thenNoExceptionIsThrown();
        thenInterfaceStatusIsNotNull();
        thenNetInterfaceTypeIs(NetworkInterfaceType.WIFI);
    }

    @Test
    public void getInterfaceStatusShouldWorkEthernetUSB() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_ACTIVATED, true);
        givenMockedDeviceList();
        givenNetworkServiceMockedForUsbInterfaceWithName("eth0");

        whenGetInterfaceStatus("eth0", this.networkService, this.commandExecutorService);

        thenNoExceptionIsThrown();
        thenInterfaceStatusIsNotNull();
        thenNetInterfaceTypeIs(NetworkInterfaceType.ETHERNET);
    }

    @Test
    public void configurationEnforcementShouldNotBeActiveOnCreation() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED,
                true);
        givenMockedDeviceList();

        thenNoExceptionIsThrown();
        thenConfigurationEnforcementIsActive(false);
    }

    @Test
    public void configurationEnforcementShouldBeActiveAfterFirstApplyCall() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED,
                true);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "eth0");
        givenNetworkConfigMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");

        givenApplyWasCalledOnceWith(this.netConfig);

        thenNoExceptionIsThrown();
        thenConfigurationEnforcementIsActive(true);
    }

    @Test
    public void configurationEnforcementShouldTriggerWithExternalChangeSignal() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED,
                true);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "eth0");
        givenNetworkConfigMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");

        givenApplyWasCalledOnceWith(this.netConfig);

        whenDeviceStateChangeSignalAppearsWith("/org/freedesktop/NetworkManager/Devices/5",
                NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_ACTIVATED),
                NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_CONFIG), new UInt32(1));

        thenNoExceptionIsThrown();
        thenConnectionUpdateIsCalledFor("eth0");
        thenActivateConnectionIsCalledFor("eth0");
        thenConfigurationEnforcementIsActive(true);
    }

    @Test
    public void configurationEnforcementShouldTriggerWithExternalDisconnect() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED,
                true);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "eth0");
        givenNetworkConfigMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");

        givenApplyWasCalledOnceWith(this.netConfig);

        whenDeviceStateChangeSignalAppearsWith("/org/freedesktop/NetworkManager/Devices/5",
                NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_DEACTIVATING),
                NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_DISCONNECTED), new UInt32(1));

        thenNoExceptionIsThrown();
        thenConnectionUpdateIsCalledFor("eth0");
        thenActivateConnectionIsCalledFor("eth0");
        thenConfigurationEnforcementIsActive(true);
    }

    @Test
    public void configurationEnforcementShouldNotTriggerWithDisconnectAfterFailure() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED,
                true);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "eth0");
        givenNetworkConfigMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");

        givenApplyWasCalledOnceWith(this.netConfig);

        whenDeviceStateChangeSignalAppearsWith("/org/freedesktop/NetworkManager/Devices/5",
                NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_FAILED),
                NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_DISCONNECTED), new UInt32(1));

        thenNoExceptionIsThrown();
        thenNetworkSettingsDidNotChangeForDevice("eth0");
        thenConfigurationEnforcementIsActive(true);
    }

    /*
     * Given
     */

    public void givenBasicMockedDbusConnector() throws DBusException, IOException {
        when(this.dbusConnection.getRemoteObject(eq("org.freedesktop.NetworkManager"), eq("/org/freedesktop/NetworkManager"), any()))
                    .thenReturn(this.mockedNetworkManager);

        this.instanceNMDbusConnector = NMDbusConnector.getInstance(this.dbusConnection);
    }

    public void givenMockedPermissions() {

        Map<String, String> tempPerms = new HashMap<>();

        tempPerms.put("test1", "testVal1");
        tempPerms.put("test2", "testVal2");
        tempPerms.put("test3", "testVal3");

        when(this.mockedNetworkManager.GetPermissions()).thenReturn(tempPerms);

    }

    public void givenMockedVersion() throws DBusException, IOException {

        Properties mockProps = mock(org.freedesktop.dbus.interfaces.Properties.class);
        when(mockProps.Get(eq("org.freedesktop.NetworkManager"), eq("Version"))).thenReturn("Mock-Version");

        doReturn(mockProps).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/org/freedesktop/NetworkManager"), eq(Properties.class));
    }

    public void givenMockedDevice(String interfaceName, NMDeviceType type, NMDeviceState state,
            Boolean hasAssociatedConnection) throws DBusException, IOException {
        Device mockedDevice1 = mock(Device.class);

        this.mockDevices.put(interfaceName, mockedDevice1);

        when(mockedDevice1.getObjectPath()).thenReturn("/mock/device/" + interfaceName);

        Generic mockedDevice1Generic = mock(Generic.class);
        when(mockedDevice1Generic.getObjectPath()).thenReturn("/mock/device/" + interfaceName);

        DBusPath mockedPath1 = mock(DBusPath.class);
        when(mockedPath1.getPath()).thenReturn("/mock/device/" + interfaceName);

        Map<String, Map<String, Variant<?>>> mockedDevice1ConnectionSetting = new HashMap<>();
        mockedDevice1ConnectionSetting.put("connection",
                Collections.singletonMap("uuid", new Variant<>("mock-uuid-123")));

        Settings mockedDevice1Settings = mock(Settings.class);
        when(mockedDevice1Settings.GetConnectionByUuid(eq("mock-uuid-123"))).thenReturn(mockedPath1);

        GetAppliedConnectionTuple mockedDevice1ConnectionTouple = mock(GetAppliedConnectionTuple.class);
        when(mockedDevice1ConnectionTouple.getConnection()).thenReturn(mockedDevice1ConnectionSetting);

        when(mockedDevice1.GetAppliedConnection(eq(new UInt32(0)))).thenReturn(mockedDevice1ConnectionTouple);

        Properties mockedProperties1 = mock(Properties.class);

        if (type == NMDeviceType.NM_DEVICE_TYPE_GENERIC && interfaceName.equals("lo")) {
            when(mockedProperties1.Get(any(), any())).thenReturn("loopback");
        }

        if (type == NMDeviceType.NM_DEVICE_TYPE_WIFI) {
            simulateIwCommandOutputs(interfaceName, mockedProperties1);
        }

        when(mockedProperties1.Get(eq("org.freedesktop.NetworkManager.Device"), eq("DeviceType")))
                .thenReturn(NMDeviceType.toUInt32(type));
        when(mockedProperties1.Get(eq("org.freedesktop.NetworkManager.Device"), eq("State")))
                .thenReturn(NMDeviceState.toUInt32(state));
        when(mockedProperties1.Get(eq("org.freedesktop.NetworkManager.Device"), eq("Interface")))
                .thenReturn(interfaceName);

        when(this.mockedNetworkManager.GetDeviceByIpIface(eq(interfaceName))).thenReturn(mockedPath1);

        doReturn(mockedDevice1).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/mock/device/" + interfaceName), eq(Device.class));
        doReturn(mockedDevice1Settings).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/org/freedesktop/NetworkManager/Settings"), eq(Settings.class));
        doReturn(mockedProperties1).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/mock/device/" + interfaceName), eq(Properties.class));
        if (Boolean.TRUE.equals(hasAssociatedConnection)) {
            this.mockConnection = mock(Connection.class, RETURNS_SMART_NULLS);
            when(this.mockConnection.GetSettings()).thenReturn(mockedDevice1ConnectionSetting);

            doReturn(this.mockConnection).when(this.dbusConnection).getRemoteObject(
                    eq("org.freedesktop.NetworkManager"), eq("/mock/device/" + interfaceName), eq(Connection.class));
        } else {
            doThrow(new DBusExecutionException("initiate mocked throw")).when(this.dbusConnection).getRemoteObject(
                    eq("org.freedesktop.NetworkManager"), eq("/mock/device/" + interfaceName), eq(Connection.class));
        }

        doReturn(mockedDevice1Generic).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/mock/device/lo"), eq(Generic.class));

        givenExtraStatusMocksFor(interfaceName, state);
    }

    public void givenExtraStatusMocksFor(String interfaceName, NMDeviceState state) throws DBusException, IOException {
        Properties mockedProperties = this.dbusConnection.getRemoteObject("org.freedesktop.NetworkManager",
                "/mock/device/" + interfaceName, Properties.class);

        when(mockedProperties.Get(eq("org.freedesktop.NetworkManager.Device"), eq("Autoconnect"))).thenReturn(true);
        when(mockedProperties.Get(eq("org.freedesktop.NetworkManager.Device"), eq("FirmwareVersion")))
                .thenReturn("firmware");
        when(mockedProperties.Get(eq("org.freedesktop.NetworkManager.Device"), eq("Driver"))).thenReturn("driver");
        when(mockedProperties.Get(eq("org.freedesktop.NetworkManager.Device"), eq("DriverVersion")))
                .thenReturn("1.0.0");
        when(mockedProperties.Get(eq("org.freedesktop.NetworkManager.Device"), eq("State")))
                .thenReturn(NMDeviceState.toUInt32(state));
        when(mockedProperties.Get(eq("org.freedesktop.NetworkManager.Device"), eq("Mtu"))).thenReturn(new UInt32(100));
        when(mockedProperties.Get(eq("org.freedesktop.NetworkManager.Device"), eq("HwAddress")))
                .thenReturn("F5:5B:32:7C:40:EA");

        DBusPath path = mock(DBusPath.class);
        when(path.getPath()).thenReturn("/");

        when(mockedProperties.Get(eq("org.freedesktop.NetworkManager.Device"), eq("Ip4Config"))).thenReturn(path);

    }

    public void givenMockedDeviceList() {

        List<DBusPath> devicePaths = new ArrayList<>();

        for (String interfaceName : this.mockDevices.keySet()) {
            devicePaths.add(this.mockedNetworkManager.GetDeviceByIpIface(interfaceName));
        }
        when(this.mockedNetworkManager.GetAllDevices()).thenReturn(devicePaths);
    }

    public void givenNetworkConfigMapWith(String key, Object value) {
        this.netConfig.put(key, value);
    }

    public void givenNetworkServiceMockedForUsbInterfaceWithName(String interfaceName) {
        this.networkService = mock(NetworkService.class);

        UsbNetDevice mockUsbNetDevice = mock(UsbNetDevice.class, RETURNS_DEEP_STUBS);

        when(this.networkService.getUsbNetDevice(interfaceName)).thenReturn(Optional.of(mockUsbNetDevice));
    }

    public void givenNetworkServiceThatAlwaysReturnsEmpty() {
        this.networkService = mock(NetworkService.class);
        when(this.networkService.getUsbNetDevice(any())).thenReturn(Optional.empty());
    }

    public void givenApplyWasCalledOnceWith(Map<String, Object> networkConfig) throws DBusException {
        this.instanceNMDbusConnector.apply(networkConfig);
        clearInvocations(this.mockedNetworkManager);
        clearInvocations(this.dbusConnection);
        clearInvocations(this.mockConnection);
    }

    /*
     * When
     */

    public void whenGetDbusConnectionIsRun() {
        this.dbusConnectionInternal = this.instanceNMDbusConnector.getDbusConnection();
    }

    public void whenCheckPermissionsIsRun() {
        this.instanceNMDbusConnector.checkPermissions();
    }

    public void whenCheckVersionIsRun() {
        try {
            this.instanceNMDbusConnector.checkVersion();
        } catch (DBusException e) {
            this.hasDBusExceptionBeenThrown = true;
        }
    }

    public void whenGetInterfacesIsCalled() {
        try {
            this.internalStringList = this.instanceNMDbusConnector.getInterfaces();
        } catch (DBusException e) {
            this.hasDBusExceptionBeenThrown = true;
        }
    }

    public void whenApplyIsCalled() {
        try {
            this.instanceNMDbusConnector.apply();
        } catch (DBusException e) {
            this.hasDBusExceptionBeenThrown = true;
        }
    }

    public void whenApplyIsCalledWith(Map<String, Object> networkConfig) {
        try {
            this.instanceNMDbusConnector.apply(networkConfig);
        } catch (DBusException e) {
            this.hasDBusExceptionBeenThrown = true;
        } catch (NoSuchElementException e) {
            this.hasNoSuchElementExceptionThrown = true;
        } catch (NullPointerException e) {
            this.hasNullPointerExceptionThrown = true;
        }
    }

    public void whenGetInterfaceStatus(String netInterface, NetworkService netService,
            CommandExecutorService commandExecutorService) {
        try {
            this.netInterface = this.instanceNMDbusConnector.getInterfaceStatus(netInterface, netService,
                    commandExecutorService);
        } catch (DBusException e) {
            this.hasDBusExceptionBeenThrown = true;
        } catch (NoSuchElementException e) {
            this.hasNoSuchElementExceptionThrown = true;
        } catch (NullPointerException e) {
            this.hasNullPointerExceptionThrown = true;
        } catch (KuraException e) {
            this.hasKuraExceptionThrown = true;
        }
    }

    private void whenDeviceStateChangeSignalAppearsWith(String dbusPath, UInt32 oldState, UInt32 newState,
            UInt32 stateReason) throws DBusException {

        Device.StateChanged signal = new Device.StateChanged(dbusPath, newState, oldState, stateReason);

        try {
            Field handlerField = NMDbusConnector.class.getDeclaredField("configurationEnforcementHandler");
            handlerField.setAccessible(true);
            NMConfigurationEnforcementHandler handler = (NMConfigurationEnforcementHandler) handlerField
                    .get(this.instanceNMDbusConnector);
            handler.handle(signal);
            handlerField.setAccessible(false);

        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Then
     */

    public void thenNoExceptionIsThrown() {
        assertFalse(this.hasDBusExceptionBeenThrown);
        assertFalse(this.hasNoSuchElementExceptionThrown);
        assertFalse(this.hasNullPointerExceptionThrown);
        assertFalse(this.hasKuraExceptionThrown);
    }

    public void thenDBusExceptionIsThrown() {
        assertTrue(this.hasDBusExceptionBeenThrown);
    }

    public void thenNullPointerExceptionIsThrown() {
        assertTrue(this.hasNullPointerExceptionThrown);
    }

    public void thenNoSuchElementExceptionIsThrown() {
        assertTrue(this.hasNoSuchElementExceptionThrown);
    }

    public void thenGetDbusConnectionReturns(DBusConnection dbusConnection) {
        assertEquals(this.dbusConnection, dbusConnection);
    }

    public void thenCheckVersionIsRun() throws DBusException, IOException {
        verify(this.dbusConnection, atLeastOnce()).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/org/freedesktop/NetworkManager"), any());
    }

    public void thenCheckPermissionsRan() {
        verify(this.mockedNetworkManager, atLeastOnce()).GetPermissions();
    }

    public void thenGetInterfacesReturn(List<String> list) {
        assertEquals(list, this.internalStringList);
    }

    public void thenDisconnectIsCalledFor(String netInterface) {
        verify(this.mockDevices.get(netInterface)).Disconnect();
    }

    public void thenConnectionUpdateIsCalledFor(String netInterface) throws DBusException {
        verify(this.mockConnection).Update(any());
    }

    public void thenActivateConnectionIsCalledFor(String netInterface) throws DBusException {
        verify(this.mockedNetworkManager).ActivateConnection(any(), any(), any());
    }

    public void thenAddAndActivateConnectionIsCalledFor(String netInterface) throws DBusException {
        verify(this.mockedNetworkManager).AddAndActivateConnection(any(), any(), any());
    }

    public void thenNetworkSettingsDidNotChangeForDevice(String netInterface) throws DBusException {
        verify(this.mockConnection, never()).Update(any());
        verify(this.mockDevices.get(netInterface), never()).Disconnect();
        verify(this.mockedNetworkManager, never()).ActivateConnection(any(), any(), any());
        verify(this.mockedNetworkManager, never()).AddAndActivateConnection(any(), any(), any());
    }

    public void thenInterfaceStatusIsNull() {
        assertNull(this.netInterface);
    }

    public void thenInterfaceStatusIsNotNull() {
        assertNotNull(this.netInterface);
    }

    public void thenNetInterfaceTypeIs(NetworkInterfaceType type) {
        assertEquals(type, this.netInterface.getType());
    }

    public void thenConfigurationEnforcementIsActive(boolean expectedValue) {
        assertEquals(expectedValue, this.instanceNMDbusConnector.configurationEnforcementIsActive());
    }

    private void simulateIwCommandOutputs(String interfaceName, Properties preMockedProperties)
            throws IOException, DBusException {
        Wireless wirelessDevice = mock(Wireless.class);
        when(wirelessDevice.getObjectPath()).thenReturn("/mock/device/" + interfaceName);

        ExitStatus exitStatus = mock(ExitStatus.class);
        when(exitStatus.getExitCode()).thenReturn(1);
        when(exitStatus.isSuccessful()).thenReturn(true);

        String[] iwRegGet = { "iw", "reg", "get" };
        Command iwRegGetCmd = new Command(iwRegGet);
        iwRegGetCmd.setTimeout(60);
        iwRegGetCmd.setOutputStream(new ByteArrayOutputStream());

        ByteArrayOutputStream iwRegGetStream = new ByteArrayOutputStream();
        iwRegGetStream.write(iwRegGetOutput.getBytes());

        CommandStatus iwRegGetCommandStatus = mock(CommandStatus.class);
        when(iwRegGetCommandStatus.getExitStatus()).thenReturn(exitStatus);
        when(iwRegGetCommandStatus.getOutputStream()).thenReturn(iwRegGetStream);

        // ---

        String[] iwWlan0Info = { "iw", "wlan0", "info" };
        Command iwWlan0InfoCmd = new Command(iwWlan0Info);
        iwWlan0InfoCmd.setOutputStream(new ByteArrayOutputStream());

        ByteArrayOutputStream iwWlan0InfoStream = new ByteArrayOutputStream();
        iwWlan0InfoStream.write(iwWlan0InfoOutput.getBytes());

        CommandStatus iwWlan0InfoStatus = mock(CommandStatus.class);
        when(iwWlan0InfoStatus.getExitStatus()).thenReturn(exitStatus);
        when(iwWlan0InfoStatus.getOutputStream()).thenReturn(iwWlan0InfoStream);

        // ---

        String[] iwPhyPhy0Info = { "iw", "phy0", "info" };
        Command iwPhyPhy0InfoCmd = new Command(iwPhyPhy0Info);
        iwPhyPhy0InfoCmd.setOutputStream(new ByteArrayOutputStream());

        ByteArrayOutputStream iwPhyPhy0InfoStream = new ByteArrayOutputStream();
        iwPhyPhy0InfoStream.write(iwPhyPhy0InfoOutput.getBytes());

        CommandStatus iwPhyPhy0InfoStatus = mock(CommandStatus.class);
        when(iwPhyPhy0InfoStatus.getExitStatus()).thenReturn(exitStatus);
        when(iwPhyPhy0InfoStatus.getOutputStream()).thenReturn(iwPhyPhy0InfoStream);

        doReturn(iwRegGetCommandStatus).when(this.commandExecutorService).execute(iwRegGetCmd);
        doReturn(iwWlan0InfoStatus).when(this.commandExecutorService).execute(iwWlan0InfoCmd);
        doReturn(iwPhyPhy0InfoStatus).when(this.commandExecutorService).execute(iwPhyPhy0InfoCmd);

        DBusPath mockedApPath = mock(DBusPath.class);
        when(mockedApPath.getPath()).thenReturn("/");

        when(preMockedProperties.Get(eq("org.freedesktop.NetworkManager.Device.Wireless"), eq("ActiveAccessPoint")))
                .thenReturn(mockedApPath);
        when(preMockedProperties.Get(eq("org.freedesktop.NetworkManager.Device.Wireless"), eq("Mode")))
                .thenReturn(new UInt32(1));
        when(preMockedProperties.Get(eq("org.freedesktop.NetworkManager.Device.Wireless"), eq("WirelessCapabilities")))
                .thenReturn(new UInt32(1));

        doReturn(wirelessDevice).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/mock/device/" + interfaceName), eq(Wireless.class));
    }

}
