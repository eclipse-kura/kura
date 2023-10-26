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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.ExitStatus;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.NetworkInterfaceType;
import org.eclipse.kura.net.status.modem.AccessTechnology;
import org.eclipse.kura.net.status.modem.Bearer;
import org.eclipse.kura.net.status.modem.BearerIpType;
import org.eclipse.kura.net.status.modem.ESimStatus;
import org.eclipse.kura.net.status.modem.ModemBand;
import org.eclipse.kura.net.status.modem.ModemCapability;
import org.eclipse.kura.net.status.modem.ModemConnectionStatus;
import org.eclipse.kura.net.status.modem.ModemInterfaceStatus;
import org.eclipse.kura.net.status.modem.ModemMode;
import org.eclipse.kura.net.status.modem.ModemModePair;
import org.eclipse.kura.net.status.modem.ModemPortType;
import org.eclipse.kura.net.status.modem.ModemPowerState;
import org.eclipse.kura.net.status.modem.RegistrationStatus;
import org.eclipse.kura.net.status.modem.Sim;
import org.eclipse.kura.net.status.modem.SimType;
import org.eclipse.kura.nm.enums.MMModemLocationSource;
import org.eclipse.kura.nm.enums.NMDeviceState;
import org.eclipse.kura.nm.enums.NMDeviceType;
import org.eclipse.kura.nm.signal.handlers.DeviceCreationLock;
import org.eclipse.kura.nm.signal.handlers.NMConfigurationEnforcementHandler;
import org.freedesktop.ModemManager1;
import org.freedesktop.NetworkManager;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.UInt64;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.modemmanager1.Modem;
import org.freedesktop.modemmanager1.modem.Location;
import org.freedesktop.networkmanager.Device;
import org.freedesktop.networkmanager.GetAppliedConnectionTuple;
import org.freedesktop.networkmanager.Settings;
import org.freedesktop.networkmanager.device.Generic;
import org.freedesktop.networkmanager.device.Vlan;
import org.freedesktop.networkmanager.device.Wired;
import org.freedesktop.networkmanager.device.Wireless;
import org.freedesktop.networkmanager.settings.Connection;
import org.junit.After;
import org.junit.Test;

import fi.w1.Wpa_supplicant1;
import fi.w1.wpa_supplicant1.Interface;

public class NMDbusConnectorTest {

    private static final String MM_MODEM_BUS_NAME = "org.freedesktop.ModemManager1.Modem";
    private final DBusConnection dbusConnection = mock(DBusConnection.class, RETURNS_SMART_NULLS);
    private final Wpa_supplicant1 mockedWpaSupplicant = mock(Wpa_supplicant1.class);
    private final NetworkManager mockedNetworkManager = mock(NetworkManager.class);
    private final ModemManager1 mockedModemManager = mock(ModemManager1.class);
    private final Settings mockedNetworkManagerSettings = mock(Settings.class);
    private NMDbusConnector instanceNMDbusConnector;
    private DBusConnection dbusConnectionInternal;
    private final CommandExecutorService commandExecutorService = mock(CommandExecutorService.class);

    private Boolean hasDBusExceptionBeenThrown = false;
    private Boolean hasNoSuchElementExceptionThrown = false;
    private Boolean hasNullPointerExceptionThrown = false;
    private Boolean hasKuraExceptionThrown = false;
    private Boolean hasIllegalArgumentExceptionThrown = false;

    private NetworkInterfaceStatus netInterface;

    private Map<String, Interface> mockedInterfaces = new HashMap<>();
    private Map<String, Connection> mockedConnections = new HashMap<>();
    private List<DBusPath> mockedConnectionDbusPathList = new ArrayList<>();

    private final Map<String, Device> mockDevices = new HashMap<>();
    private Connection mockConnection;

    private List<String> internalStringList;
    private final Map<String, Object> netConfig = new HashMap<>();
    private Location mockModemLocation;
    
    private static String basicNmVersion = "1.40";

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
        givenMockedDevice("eth0", "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, false, false);
        givenMockedDevice("wlan0", "wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, false, false);
        givenMockedDevice("eth0.10", "eth0.10", NMDeviceType.NM_DEVICE_TYPE_VLAN,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, false, false);
        givenMockedDeviceList();

        whenGetInterfaceIdsIsCalled();

        thenNoExceptionIsThrown();
        thenGetInterfacesReturn(Arrays.asList("wlan0", "eth0.10", "eth0"));
    }

    @Test
    public void applyShouldDoNothingWithNoCache() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, false, false);
        givenMockedDevice("wlan0", "wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, false, false);
        givenMockedDeviceList();

        whenApplyIsCalled();

        thenNoExceptionIsThrown();
        thenNetworkSettingsDidNotChangeForDevice("eth0");
        thenNetworkSettingsDidNotChangeForDevice("wlan0");
    }

    @Test
    public void applyShouldThrowWithNullMap() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, false, false);
        givenMockedDevice("wlan0", "wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, false, false);
        givenMockedDeviceList();

        whenApplyIsCalledWith(null);

        thenNullPointerExceptionIsThrown();
    }

    @Test
    public void applyShouldDoNothingWithEmptyMap() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, false, false);
        givenMockedDevice("wlan0", "wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, false, false);
        givenMockedDeviceList();

        whenApplyIsCalledWith(new HashMap<String, Object>());

        thenNoExceptionIsThrown();
        thenNetworkSettingsDidNotChangeForDevice("eth0");
        thenNetworkSettingsDidNotChangeForDevice("wlan0");
    }

    @Test
    public void applyShouldDoNothingWithNonExistingDeviceId() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, false, false);
        givenMockedDevice("wlan0", "wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, false, false);
        givenMockedDeviceList();

        whenApplySingleIsCalledWith("eth1");

        thenNoExceptionIsThrown();
        thenNetworkSettingsDidNotChangeForDevice("eth0");
        thenNetworkSettingsDidNotChangeForDevice("wlan0");
    }

    @Test
    public void applyShouldThrowWithNullDeviceId() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, false, false);
        givenMockedDevice("wlan0", "wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, false, false);
        givenMockedDeviceList();

        whenApplySingleIsCalledWith(null);

        thenIllegalArgumentExceptionIsThrown();
    }

    @Test
    public void applyShouldThrowWithEmptyDeviceId() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, false, false);
        givenMockedDevice("wlan0", "wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, false, false);
        givenMockedDeviceList();

        whenApplySingleIsCalledWith("");

        thenIllegalArgumentExceptionIsThrown();
    }

    @Test
    public void applyShouldDoNothingWithEnabledUnsupportedDevices() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("unused0", "unused0", NMDeviceType.NM_DEVICE_TYPE_UNUSED1,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, false, false);
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
        givenMockedDevice("eth0", "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, false, false);
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
        givenMockedDevice("eth0", "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, false, false, false);
        givenMockedDeviceList();
        givenMockToPrepNetworkManagerToAllowDeviceToCreateNewConnection();

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
        givenMockedDevice("eth0", "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_ACTIVATED,
                true, false, false);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "eth,");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusDisabled");

        whenApplyIsCalledWith(this.netConfig);

        thenNoExceptionIsThrown();
        thenDisconnectIsCalledFor("eth0");
    }
    
    @Test
    public void applyShouldWorkWithVlanCreation() throws DBusException, IOException, TimeoutException {
    	givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, false, false, false);
        /*givenMockedDevice("myVlan", "myVlan", NMDeviceType.NM_DEVICE_TYPE_ETHERNET,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, false, false, false);*/
        givenMockedDeviceList();
        
        givenMockedDeviceOnDeviceCreationLock("myVlan", "myVlan", NMDeviceType.NM_DEVICE_TYPE_VLAN,
                NMDeviceState.NM_DEVICE_STATE_ACTIVATED, true, false, false);
        givenMockToPrepNetworkManagerToAllowDeviceToCreateNewConnection();
        
        givenNetworkConfigMapWith("net.interfaces", "eth0,myVlan");
        givenNetworkConfigMapWith("net.interface.myVlan.type", "VLAN");
        givenNetworkConfigMapWith("net.interface.myVlan.config.dhcpClient4.enabled", false);
        givenNetworkConfigMapWith("net.interface.myVlan.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenNetworkConfigMapWith("net.interface.myVlan.config.ip4.address", "192.168.0.12");
        givenNetworkConfigMapWith("net.interface.myVlan.config.ip4.prefix", (short) 25);
        givenNetworkConfigMapWith("net.interface.myVlan.config.ip4.dnsServers", "1.1.1.1");
        givenNetworkConfigMapWith("net.interface.myVlan.config.ip4.gateway", "192.168.0.1");
        givenNetworkConfigMapWith("net.interface.myVlan.config.vlan.parent", "eth0");
        givenNetworkConfigMapWith("net.interface.myVlan.config.vlan.id", 55);
        givenNetworkConfigMapWith("net.interface.myVlan.config.vlan.flags", 2);
        givenNetworkConfigMapWith("net.interface.myVlan.config.vlan.egress", "2:3");
        
        whenApplyIsCalledWith(this.netConfig);

        thenNoExceptionIsThrown();
        
        thenAddConnectionIsCalledFor("myVlan");
    }
    
    @Test
    public void applyShouldWorkWithExistingVlan() throws DBusException, IOException{
    	givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, false, false, false);
        givenMockedDevice("myVlan", "myVlan", NMDeviceType.NM_DEVICE_TYPE_VLAN,
                NMDeviceState.NM_DEVICE_STATE_ACTIVATED, true, false, false);//
        givenMockedDeviceList();
        
        
        givenNetworkConfigMapWith("net.interfaces", "eth0,myVlan");
        givenNetworkConfigMapWith("net.interface.myVlan.type", "VLAN");
        givenNetworkConfigMapWith("net.interface.myVlan.config.dhcpClient4.enabled", false);
        givenNetworkConfigMapWith("net.interface.myVlan.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenNetworkConfigMapWith("net.interface.myVlan.config.ip4.address", "192.168.0.12");
        givenNetworkConfigMapWith("net.interface.myVlan.config.ip4.prefix", (short) 25);
        givenNetworkConfigMapWith("net.interface.myVlan.config.ip4.dnsServers", "1.1.1.1");
        givenNetworkConfigMapWith("net.interface.myVlan.config.ip4.gateway", "192.168.0.1");
        givenNetworkConfigMapWith("net.interface.myVlan.config.vlan.parent", "eth0");
        givenNetworkConfigMapWith("net.interface.myVlan.config.vlan.id", 55);
        givenNetworkConfigMapWith("net.interface.myVlan.config.vlan.flags", 2);
        givenNetworkConfigMapWith("net.interface.myVlan.config.vlan.egress", "2:3");
        
        whenApplyIsCalledWith(this.netConfig);

        thenNoExceptionIsThrown();
        thenConnectionUpdateIsCalledFor("myVlan");
    }

    @Test
    public void applyShouldNotDisableLoopbackDevice() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("lo", "lo", NMDeviceType.NM_DEVICE_TYPE_LOOPBACK, NMDeviceState.NM_DEVICE_STATE_ACTIVATED,
                true, false, false);
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
        givenMockedDevice("lo", "lo", NMDeviceType.NM_DEVICE_TYPE_GENERIC, NMDeviceState.NM_DEVICE_STATE_ACTIVATED,
                true, false, false);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "lo,");
        givenNetworkConfigMapWith("net.interface.lo.config.ip4.status", "netIPv4StatusDisabled");

        whenApplyIsCalledWith(this.netConfig);

        thenNoExceptionIsThrown();
        thenNetworkSettingsDidNotChangeForDevice("lo");
    }

    @Test
    public void applyShouldWorkWithDisabledModem() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("1-5", "ttyACM17", NMDeviceType.NM_DEVICE_TYPE_MODEM, NMDeviceState.NM_DEVICE_STATE_ACTIVATED,
                true, false, false);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "1-5,");
        givenNetworkConfigMapWith("net.interface.1-5.config.ip4.status", "netIPv4StatusDisabled");
        givenNetworkConfigMapWith("net.interface.1-5.config.gpsEnabled", false);
        givenNetworkConfigMapWith("net.interface.1-5.config.resetTimeout", 0);

        whenApplyIsCalledWith(this.netConfig);

        thenNoExceptionIsThrown();
        thenDisconnectIsCalledFor("ttyACM17");
        thenLocationSetupWasCalledWith(EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_NONE), false);
    }

    @Test
    public void applyShouldEnableGPSEvenIfModemIsDisabled() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("1-5", "ttyACM17", NMDeviceType.NM_DEVICE_TYPE_MODEM, NMDeviceState.NM_DEVICE_STATE_ACTIVATED,
                true, false, false);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "1-5,");
        givenNetworkConfigMapWith("net.interface.1-5.config.ip4.status", "netIPv4StatusDisabled");
        givenNetworkConfigMapWith("net.interface.1-5.config.gpsEnabled", true);
        givenNetworkConfigMapWith("net.interface.1-5.config.resetTimeout", 0);

        whenApplyIsCalledWith(this.netConfig);

        thenNoExceptionIsThrown();
        thenDisconnectIsCalledFor("ttyACM17");
        thenLocationSetupWasCalledWith(EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_UNMANAGED), false);
    }

    @Test
    public void applyShouldWorkWithEnabledModem() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("1-5", "ttyACM17", NMDeviceType.NM_DEVICE_TYPE_MODEM, NMDeviceState.NM_DEVICE_STATE_ACTIVATED,
                true, false, false);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "1-5,");
        givenNetworkConfigMapWith("net.interface.1-5.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenNetworkConfigMapWith("net.interface.1-5.config.dhcpClient4.enabled", true);
        givenNetworkConfigMapWith("net.interface.1-5.config.apn", "myAwesomeAPN");
        givenNetworkConfigMapWith("net.interface.1-5.config.gpsEnabled", false);
        givenNetworkConfigMapWith("net.interface.1-5.config.resetTimeout", 0);

        whenApplyIsCalledWith(this.netConfig);

        thenNoExceptionIsThrown();
        thenConnectionUpdateIsCalledFor("ttyACM17");
        thenActivateConnectionIsCalledFor("ttyACM17");
        thenLocationSetupWasCalledWith(EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_NONE), false);
    }

    @Test
    public void applyShouldWorkWithModemWithEnabledGPS() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("1-5", "ttyACM17", NMDeviceType.NM_DEVICE_TYPE_MODEM, NMDeviceState.NM_DEVICE_STATE_ACTIVATED,
                true, false, false);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "1-5,");
        givenNetworkConfigMapWith("net.interface.1-5.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenNetworkConfigMapWith("net.interface.1-5.config.dhcpClient4.enabled", true);
        givenNetworkConfigMapWith("net.interface.1-5.config.apn", "myAwesomeAPN");
        givenNetworkConfigMapWith("net.interface.1-5.config.gpsEnabled", true);
        givenNetworkConfigMapWith("net.interface.1-5.config.resetTimeout", 0);

        whenApplyIsCalledWith(this.netConfig);

        thenNoExceptionIsThrown();
        thenConnectionUpdateIsCalledFor("ttyACM17");
        thenActivateConnectionIsCalledFor("ttyACM17");
        thenLocationSetupWasCalledWith(EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_UNMANAGED), false);
    }

    @Test
    public void applyShouldDisableGPSWithMissingGPSConfiguration() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("1-5", "ttyACM17", NMDeviceType.NM_DEVICE_TYPE_MODEM, NMDeviceState.NM_DEVICE_STATE_ACTIVATED,
                true, false, false);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "1-5,");
        givenNetworkConfigMapWith("net.interface.1-5.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenNetworkConfigMapWith("net.interface.1-5.config.dhcpClient4.enabled", true);
        givenNetworkConfigMapWith("net.interface.1-5.config.apn", "myAwesomeAPN");
        givenNetworkConfigMapWith("net.interface.1-5.config.resetTimeout", 0);

        whenApplyIsCalledWith(this.netConfig);

        thenNoExceptionIsThrown();
        thenConnectionUpdateIsCalledFor("ttyACM17");
        thenActivateConnectionIsCalledFor("ttyACM17");
        thenLocationSetupWasCalledWith(EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_NONE), false);
    }

    @Test
    public void getEthernetInterfaceStatusShouldWork() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_ACTIVATED,
                true, false, false);
        givenMockedDeviceList();

        whenGetInterfaceStatus("eth0", this.commandExecutorService);

        thenNoExceptionIsThrown();
        thenInterfaceStatusIsNotNull();
        thenNetInterfaceTypeIs(NetworkInterfaceType.ETHERNET);
    }

    @Test
    public void getLoopbackInterfaceStatusShouldWork() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("lo", "lo", NMDeviceType.NM_DEVICE_TYPE_LOOPBACK, NMDeviceState.NM_DEVICE_STATE_FAILED, true,
                false, false);
        givenMockedDeviceList();

        whenGetInterfaceStatus("lo", this.commandExecutorService);

        thenNoExceptionIsThrown();
        thenInterfaceStatusIsNotNull();
        thenNetInterfaceTypeIs(NetworkInterfaceType.LOOPBACK);
    }

    @Test
    public void getUnsupportedInterfaceStatusShouldWork() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("unused0", "unused0", NMDeviceType.NM_DEVICE_TYPE_UNUSED1,
                NMDeviceState.NM_DEVICE_STATE_FAILED, true, false, false);
        givenMockedDeviceList();

        whenGetInterfaceStatus("unused0", this.commandExecutorService);

        thenNoExceptionIsThrown();
        thenInterfaceStatusIsNull();
    }

    @Test
    public void getWirelessInterfaceStatusShouldWork() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("wlan0", "wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI, NMDeviceState.NM_DEVICE_STATE_FAILED,
                true, false, false);
        givenMockedDeviceList();

        whenGetInterfaceStatus("wlan0", this.commandExecutorService);

        thenNoExceptionIsThrown();
        thenInterfaceStatusIsNotNull();
        thenNetInterfaceTypeIs(NetworkInterfaceType.WIFI);
    }

    @Test
    public void getModemInterfaceStatusShouldWork() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("1-5", "ttyACM17", NMDeviceType.NM_DEVICE_TYPE_MODEM, NMDeviceState.NM_DEVICE_STATE_FAILED,
                true, true, true);
        givenMockedDeviceList();

        whenGetInterfaceStatus("1-5", this.commandExecutorService);

        thenNoExceptionIsThrown();
        thenInterfaceStatusIsNotNull();
        thenNetInterfaceTypeIs(NetworkInterfaceType.MODEM);
        thenModemStatusHasCorrectValues(true, true);
    }

    @Test
    public void getModemInterfaceStatusWithoutBearersShouldWork() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("1-5", "ttyACM17", NMDeviceType.NM_DEVICE_TYPE_MODEM, NMDeviceState.NM_DEVICE_STATE_FAILED,
                true, false, true);
        givenMockedDeviceList();

        whenGetInterfaceStatus("1-5", this.commandExecutorService);

        thenNoExceptionIsThrown();
        thenInterfaceStatusIsNotNull();
        thenNetInterfaceTypeIs(NetworkInterfaceType.MODEM);
        thenModemStatusHasCorrectValues(false, true);
    }

    @Test
    public void getModemInterfaceStatusWithoutSimsShouldWork() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("1-5", "ttyACM17", NMDeviceType.NM_DEVICE_TYPE_MODEM, NMDeviceState.NM_DEVICE_STATE_FAILED,
                true, true, false);
        givenMockedDeviceList();

        whenGetInterfaceStatus("1-5", this.commandExecutorService);

        thenNoExceptionIsThrown();
        thenInterfaceStatusIsNotNull();
        thenNetInterfaceTypeIs(NetworkInterfaceType.MODEM);
        thenModemStatusHasCorrectValues(true, false);
    }

    @Test
    public void configurationEnforcementShouldNotBeActiveWithEmptyConfigurationCache()
            throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, true, true);
        givenMockedDeviceList();

        thenNoExceptionIsThrown();
        thenConfigurationEnforcementIsActive(false);
    }

    @Test
    public void configurationEnforcementShouldBeActiveAfterConfigurationCacheGetsPopulated()
            throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth0", "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, true, true);
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
        givenMockedDevice("eth0", "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, true, true);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "eth0");
        givenNetworkConfigMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");

        givenApplyWasCalledOnceWith(this.netConfig);

        whenDeviceStateChangeSignalAppearsWith("/mock/device/eth0",
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
        givenMockedDevice("eth0", "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, true, true);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "eth0");
        givenNetworkConfigMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");

        givenApplyWasCalledOnceWith(this.netConfig);

        whenDeviceStateChangeSignalAppearsWith("/mock/device/eth0",
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
        givenMockedDevice("eth0", "eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET,
                NMDeviceState.NM_DEVICE_STATE_DISCONNECTED, true, true, true);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "eth0");
        givenNetworkConfigMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");

        givenApplyWasCalledOnceWith(this.netConfig);

        whenDeviceStateChangeSignalAppearsWith("/mock/device/eth0",
                NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_FAILED),
                NMDeviceState.toUInt32(NMDeviceState.NM_DEVICE_STATE_DISCONNECTED), new UInt32(1));

        thenNoExceptionIsThrown();
        thenNetworkSettingsDidNotChangeForDevice("eth0");
        thenConfigurationEnforcementIsActive(true);
    }

    @Test
    public void applyingConfigurationShouldCleanUnusedConnectionsIfActiveConnectionExists()
            throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth1", "eth1", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_ACTIVATED,
                true, false, false);
        givenMockedDeviceList();

        givenMockedAssociatedConnection("kura-eth1-connection", "uuid-1234", "eth1", "/connection/path/mock/0");
        givenMockedConnection("kura-eth1-connection", "uuid-1234", "eth1", "/connection/path/mock/1");
        givenMockedConnection("kura-eth1-connection", "uuid-4345", "eth1", "/connection/path/mock/2");
        givenMockedConnection("kura-eth1-connection", "uuid-5466", "eth1", "/connection/path/mock/3");
        givenMockedConnection("kura-eth1-connection", "uuid-3453", "eth1", "/connection/path/mock/4");
        givenMockedConnection("kura-eth0-connection", "uuid-3454", "eth0", "/connection/path/mock/5");

        givenNetworkConfigMapWith("net.interfaces", "eth1");
        givenNetworkConfigMapWith("net.interface.eth1.config.dhcpClient4.enabled", true);
        givenNetworkConfigMapWith("net.interface.eth1.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenNetworkConfigMapWith("net.interface.eth1.config.ip6.status", "netIPv4StatusDisabled");

        whenApplyIsCalledWith(this.netConfig);

        thenConnectionIsNotDeleted("/connection/path/mock/0");
        thenConnectionIsDeleted("/connection/path/mock/1");
        thenConnectionIsDeleted("/connection/path/mock/2");
        thenConnectionIsDeleted("/connection/path/mock/3");
        thenConnectionIsDeleted("/connection/path/mock/4");
        thenConnectionIsNotDeleted("/connection/path/mock/5");
    }

    @Test
    public void applyingConfigurationShouldDeleteExistingExtraAvailableConnections() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("eth1", "eth1", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_ACTIVATED,
                true, false, false);
        givenMockedDeviceList();

        givenMockedConnection("kura-eth1-connection", "uuid-1234", "wlan0", "/connection/path/mock/1");
        givenMockedConnection("kura-eth1-connection", "uuid-4345", "wlan0", "/connection/path/mock/2");
        givenMockedConnection("kura-eth1-connection", "uuid-5466", "wlan0", "/connection/path/mock/3");
        givenMockedConnection("kura-eth1-connection", "uuid-3453", "wlan0", "/connection/path/mock/4");
        givenMockedConnection("kura-eth0-connection", "uuid-3454", "eth0", "/connection/path/mock/5");

        givenNetworkConfigMapWith("net.interfaces", "eth1");
        givenNetworkConfigMapWith("net.interface.eth1.config.dhcpClient4.enabled", true);
        givenNetworkConfigMapWith("net.interface.eth1.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenNetworkConfigMapWith("net.interface.eth1.config.ip6.status", "netIPv4StatusDisabled");

        whenApplyIsCalledWith(this.netConfig);

        thenConnectionIsNotDeleted("/connection/path/mock/1");
        thenConnectionIsDeleted("/connection/path/mock/2");
        thenConnectionIsDeleted("/connection/path/mock/3");
        thenConnectionIsDeleted("/connection/path/mock/4");
        thenConnectionIsNotDeleted("/connection/path/mock/5");
    }

    @Test
    public void applyingDisableConfigurationShouldCleanUnusedAssociatedConnections() throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("wlan0", "wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI, NMDeviceState.NM_DEVICE_STATE_ACTIVATED,
                true, false, false);
        givenMockedDeviceList();

        givenMockedConnection("kura-wlan0-connection", "uuid-1234", "wlan0", "/connection/path/mock/1");
        givenMockedConnection("kura-wlan0-connection", "uuid-4345", "wlan0", "/connection/path/mock/2");
        givenMockedConnection("kura-wlan0-connection", "uuid-5466", "wlan0", "/connection/path/mock/3");
        givenMockedConnection("kura-wlan0-connection", "uuid-3453", "wlan0", "/connection/path/mock/4");
        givenMockedConnection("kura-eth0-connection", "uuid-3454", "eth0", "/connection/path/mock/5");

        givenNetworkConfigMapWith("net.interfaces", "wlan0");
        givenNetworkConfigMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusDisabled");

        whenApplyIsCalledWith(this.netConfig);

        thenConnectionIsDeleted("/connection/path/mock/1");
        thenConnectionIsDeleted("/connection/path/mock/2");
        thenConnectionIsDeleted("/connection/path/mock/3");
        thenConnectionIsDeleted("/connection/path/mock/4");
        thenConnectionIsNotDeleted("/connection/path/mock/5");
    }

    @Test
    public void applyingDisableConfigurationShouldCleanUnusedConnectionsIfActiveConnectionExists()
            throws DBusException, IOException {
        givenBasicMockedDbusConnector();
        givenMockedDevice("wlan0", "wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI, NMDeviceState.NM_DEVICE_STATE_ACTIVATED,
                true, false, false);
        givenMockedDeviceList();

        givenMockedAssociatedConnection("kura-wlan0-connection", "uuid-1234", "wlan0", "/connection/path/mock/0");
        givenMockedConnection("kura-wlan0-connection", "uuid-1234", "wlan0", "/connection/path/mock/1");
        givenMockedConnection("kura-wlan0-connection", "uuid-4345", "wlan0", "/connection/path/mock/2");
        givenMockedConnection("kura-wlan0-connection", "uuid-5466", "wlan0", "/connection/path/mock/3");
        givenMockedConnection("kura-wlan0-connection", "uuid-3453", "wlan0", "/connection/path/mock/4");
        givenMockedConnection("kura-eth0-connection", "uuid-3454", "eth0", "/connection/path/mock/5");

        givenNetworkConfigMapWith("net.interfaces", "wlan0");
        givenNetworkConfigMapWith("net.interface.wlan0.config.ip4.status", "netIPv4StatusDisabled");

        whenApplyIsCalledWith(this.netConfig);

        thenConnectionIsDeleted("/connection/path/mock/0");
        thenConnectionIsDeleted("/connection/path/mock/1");
        thenConnectionIsDeleted("/connection/path/mock/2");
        thenConnectionIsDeleted("/connection/path/mock/3");
        thenConnectionIsDeleted("/connection/path/mock/4");
        thenConnectionIsNotDeleted("/connection/path/mock/5");
    }

    @Test
    public void shouldTriggerWirelessNetworkScan() throws DBusException, IOException {

        givenBasicMockedDbusConnector();
        givenMockedDevice("wlan0", "wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI, NMDeviceState.NM_DEVICE_STATE_ACTIVATED,
                true, false, false);
        givenMockedDeviceList();

        whenGetInterfaceStatusWithRecompute("wlan0", this.commandExecutorService);

        thenNoExceptionIsThrown();
        thenInterfaceStatusIsNotNull();
        thenNetInterfaceTypeIs(NetworkInterfaceType.WIFI);
        thenScanIsTriggered("wlan0");
    }

    /*
     * Given
     */

    private void givenBasicMockedDbusConnector() throws DBusException, IOException {
        when(this.dbusConnection.getRemoteObject("org.freedesktop.NetworkManager",
                "/org/freedesktop/NetworkManager", NetworkManager.class))
                .thenReturn(this.mockedNetworkManager);

        when(this.dbusConnection.getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/org/freedesktop/NetworkManager/Settings"), any()))
                .thenReturn(this.mockedNetworkManagerSettings);

        when(this.dbusConnection.getRemoteObject(eq("org.freedesktop.ModemManager1"),
                eq("/org/freedesktop/ModemManager1"), any()))
                .thenReturn(this.mockedModemManager);
        
        when(this.dbusConnection.getRemoteObject(eq("fi.w1.wpa_supplicant1"),
                eq("/fi/w1/wpa_supplicant1"), any())).thenReturn(this.mockedWpaSupplicant);
        
        Properties nmProperties = mock(Properties.class);
        when(nmProperties.Get("/org/freedesktop/NetworkManager","Version"))
                .thenReturn(basicNmVersion);
        
        when(this.dbusConnection.getRemoteObject("org.freedesktop.NetworkManager",
                "/org/freedesktop/NetworkManager", Properties.class))
                .thenReturn(nmProperties);
        
        this.instanceNMDbusConnector = NMDbusConnector.getInstance(this.dbusConnection);

    }

    private void givenMockedPermissions() {

        Map<String, String> tempPerms = new HashMap<>();

        tempPerms.put("test1", "testVal1");
        tempPerms.put("test2", "testVal2");
        tempPerms.put("test3", "testVal3");

        when(this.mockedNetworkManager.GetPermissions()).thenReturn(tempPerms);

    }

    private void givenMockedVersion() throws DBusException, IOException {

        Properties mockProps = mock(org.freedesktop.dbus.interfaces.Properties.class);
        when(mockProps.Get("org.freedesktop.NetworkManager", "Version")).thenReturn("Mock-Version");

        doReturn(mockProps).when(this.dbusConnection).getRemoteObject("org.freedesktop.NetworkManager",
                "/org/freedesktop/NetworkManager", Properties.class);
    }

    private void givenMockedDevice(String deviceId, String interfaceId, NMDeviceType type, NMDeviceState state,
            Boolean hasAssociatedConnection, boolean hasBearers, boolean hasSims) throws DBusException, IOException {
        Device mockedDevice1 = mock(Device.class);

        this.mockDevices.put(interfaceId, mockedDevice1);

        when(mockedDevice1.getObjectPath()).thenReturn("/mock/device/" + interfaceId);

        Generic mockedDevice1Generic = mock(Generic.class);
        when(mockedDevice1Generic.getObjectPath()).thenReturn("/mock/device/" + interfaceId);

        DBusPath mockedPath1 = mock(DBusPath.class);
        when(mockedPath1.getPath()).thenReturn("/mock/device/" + interfaceId);

        Map<String, Map<String, Variant<?>>> mockedDevice1ConnectionSetting = new HashMap<>();
        mockedDevice1ConnectionSetting.put("connection",
                Collections.singletonMap("uuid", new Variant<>("mock-uuid-123")));

        when(this.mockedNetworkManagerSettings.GetConnectionByUuid("mock-uuid-123")).thenReturn(mockedPath1);

        GetAppliedConnectionTuple mockedDevice1ConnectionTouple = mock(GetAppliedConnectionTuple.class);
        when(mockedDevice1ConnectionTouple.getConnection()).thenReturn(mockedDevice1ConnectionSetting);

        when(mockedDevice1.GetAppliedConnection(new UInt32(0))).thenReturn(mockedDevice1ConnectionTouple);

        Properties mockedProperties1 = mock(Properties.class);

        if (type == NMDeviceType.NM_DEVICE_TYPE_GENERIC && interfaceId.equals("lo")) {
            when(mockedProperties1.Get(any(), any())).thenReturn("loopback");
        }

        if (type == NMDeviceType.NM_DEVICE_TYPE_WIFI) {
            simulateIwCommandOutputs(interfaceId, mockedProperties1);
            Interface mockedInterface = mock(Interface.class);

            this.mockedInterfaces.put(interfaceId, mockedInterface);

            DBusPath mockedInterfaceDbusPath = new DBusPath("/mock/device/" + interfaceId);

            when(this.mockedWpaSupplicant.GetInterface(interfaceId)).thenReturn(mockedInterfaceDbusPath);

            doReturn(mockedInterface).when(this.dbusConnection).getRemoteObject("fi.w1.wpa_supplicant1",
                    mockedInterfaceDbusPath.getPath(), Interface.class);

        }

        if (type == NMDeviceType.NM_DEVICE_TYPE_ETHERNET) {
            Wired wiredDevice = mock(Wired.class);
            when(wiredDevice.getObjectPath()).thenReturn("/mock/device/" + interfaceId);

            doReturn(wiredDevice).when(this.dbusConnection).getRemoteObject("org.freedesktop.NetworkManager",
                    "/mock/device/" + interfaceId, Wired.class);
        }
        
        if (type == NMDeviceType.NM_DEVICE_TYPE_VLAN) {
        	Vlan vlanDevice = mock(Vlan.class);
        	when(vlanDevice.getObjectPath()).thenReturn("/mock/device/" + interfaceId);
        	
        	doReturn(vlanDevice).when(this.dbusConnection).getRemoteObject("org.freedesktop.NetworkManager",
                    "/mock/device/" + interfaceId, Vlan.class);
        }

        when(mockedProperties1.Get("org.freedesktop.NetworkManager.Device", "DeviceType"))
                .thenReturn(NMDeviceType.toUInt32(type));
        when(mockedProperties1.Get("org.freedesktop.NetworkManager.Device", "State"))
                .thenReturn(NMDeviceState.toUInt32(state));
        when(mockedProperties1.Get("org.freedesktop.NetworkManager.Device", "Interface")).thenReturn(interfaceId);

        when(this.mockedNetworkManager.GetDeviceByIpIface(interfaceId)).thenReturn(mockedPath1);

        doReturn(mockedDevice1).when(this.dbusConnection).getRemoteObject("org.freedesktop.NetworkManager",
                "/mock/device/" + interfaceId, Device.class);
        doReturn(mockedProperties1).when(this.dbusConnection).getRemoteObject("org.freedesktop.NetworkManager",
                "/mock/device/" + interfaceId, Properties.class);
        if (hasAssociatedConnection) {
            this.mockConnection = mock(Connection.class, RETURNS_SMART_NULLS);
            when(this.mockConnection.GetSettings()).thenReturn(mockedDevice1ConnectionSetting);

            doReturn(this.mockConnection).when(this.dbusConnection).getRemoteObject("org.freedesktop.NetworkManager",
                    "/mock/device/" + interfaceId, Connection.class);
        } else {
            doThrow(new DBusExecutionException("initiate mocked throw")).when(this.dbusConnection)
                    .getRemoteObject("org.freedesktop.NetworkManager", "/mock/device/" + interfaceId, Connection.class);
        }

        doReturn(mockedDevice1Generic).when(this.dbusConnection).getRemoteObject("org.freedesktop.NetworkManager",
                "/mock/device/lo", Generic.class);

        Properties mockedProperties = this.dbusConnection.getRemoteObject("org.freedesktop.NetworkManager",
                "/mock/device/" + interfaceId, Properties.class);
        givenExtraStatusMocksFor(interfaceId, state, mockedProperties);
        if (type == NMDeviceType.NM_DEVICE_TYPE_MODEM) {
            givenModemMocksFor(deviceId, interfaceId, mockedProperties, hasBearers, hasSims);
        }

    }
    

    public void givenMockToPrepNetworkManagerToAllowDeviceToCreateNewConnection() throws DBusException {
        DBusPath newConnectionPath = mock(DBusPath.class);
        when(newConnectionPath.getPath()).thenReturn("/mock/Connection/path/newly/created");

        when(this.mockedNetworkManagerSettings.AddConnection(any())).thenReturn(newConnectionPath);

        doReturn(mock(Connection.class)).when(this.dbusConnection).getRemoteObject("org.freedesktop.NetworkManager",
                "/mock/Connection/path/newly/created", Connection.class);
    }
    
    public void givenMockedDeviceOnDeviceCreationLock(String deviceId, String interfaceId, NMDeviceType type, NMDeviceState state,
            Boolean hasAssociatedConnection, boolean hasBearers, boolean hasSims) throws DBusException, TimeoutException {
        DeviceCreationLock dcLock = mock(DeviceCreationLock.class);
        when(dcLock.waitForDeviceCreation(anyLong())).then(invocation -> {
        	givenMockedDevice(deviceId, interfaceId, type, state, hasAssociatedConnection, hasBearers, hasSims);
        	givenMockedDeviceList();
        	return Optional.ofNullable(mockDevices.get(deviceId));
        });
        when(dcLock.waitForDeviceCreation()).then(invocation -> {
        	givenMockedDevice(deviceId, interfaceId, type, state, hasAssociatedConnection, hasBearers, hasSims);
        	givenMockedDeviceList();
        	return Optional.ofNullable(mockDevices.get(deviceId));
        });
    }

    public void givenMockedConnection(String connectionId, String connectionUuid, String interfaceName,
            String connectionPath) throws DBusException {

        if (this.mockedConnectionDbusPathList.isEmpty()) {
            when(this.mockedNetworkManagerSettings.ListConnections()).thenReturn(this.mockedConnectionDbusPathList);

            DBusPath mockUuidPath = mock(DBusPath.class);
            when(mockUuidPath.getPath()).thenReturn("/unused/connection/path");

            when(this.mockedNetworkManagerSettings.GetConnectionByUuid(any())).thenReturn(mockUuidPath);

            doThrow(DBusExecutionException.class).when(this.dbusConnection)
                    .getRemoteObject("org.freedesktop.NetworkManager", "/unused/connection/path", Connection.class);
        }

        DBusPath mockPath = mock(DBusPath.class);
        when(mockPath.getPath()).thenReturn(connectionPath);

        this.mockedConnectionDbusPathList.add(mockPath);

        Map<String, Map<String, Variant<?>>> connectionSettings = new HashMap<>();

        Map<String, Variant<?>> variantConfig = new HashMap<>();

        variantConfig.put("id", new Variant<>(connectionId));
        variantConfig.put("uuid", new Variant<>(connectionUuid));
        connectionSettings.put("connection", variantConfig);

        Connection mockNewConnection = mock(Connection.class);
        when(mockNewConnection.GetSettings()).thenReturn(connectionSettings);
        when(mockNewConnection.getObjectPath()).thenReturn(connectionPath);

        doReturn(mockNewConnection).when(this.dbusConnection).getRemoteObject("org.freedesktop.NetworkManager",
                connectionPath, Connection.class);

        this.mockedConnections.put(connectionPath, mockNewConnection);

    }

    public void givenMockedAssociatedConnection(String connectionId, String connectionUuid, String interfaceName,
            String connectionPath) throws DBusException {

        Connection mockAssociatedConnection = mock(Connection.class);

        if (this.mockedConnectionDbusPathList.isEmpty()) {
            when(this.mockedNetworkManagerSettings.ListConnections()).thenReturn(this.mockedConnectionDbusPathList);

            DBusPath mockUuidPath = mock(DBusPath.class);
            when(mockUuidPath.getPath()).thenReturn("/path/to/Associated/Connection");

            when(this.mockedNetworkManagerSettings.GetConnectionByUuid(any())).thenReturn(mockUuidPath);

            doReturn(mockAssociatedConnection).when(this.dbusConnection).getRemoteObject(
                    "org.freedesktop.NetworkManager", "/path/to/Associated/Connection", Connection.class);
        }

        DBusPath mockPath = mock(DBusPath.class);
        when(mockPath.getPath()).thenReturn(connectionPath);

        this.mockedConnectionDbusPathList.add(mockPath);

        Map<String, Map<String, Variant<?>>> connectionSettings = new HashMap<>();

        Map<String, Variant<?>> variantConfig = new HashMap<>();

        variantConfig.put("id", new Variant<>(connectionId));
        variantConfig.put("uuid", new Variant<>(connectionUuid));
        connectionSettings.put("connection", variantConfig);

        when(mockAssociatedConnection.GetSettings()).thenReturn(connectionSettings);
        when(mockAssociatedConnection.getObjectPath()).thenReturn(connectionPath);

        doReturn(mockAssociatedConnection).when(this.dbusConnection).getRemoteObject("org.freedesktop.NetworkManager",
                connectionPath, Connection.class);

        this.mockedConnections.put(connectionPath, mockAssociatedConnection);

    }

    private void givenExtraStatusMocksFor(String interfaceName, NMDeviceState state, Properties mockedProperties)
            throws DBusException, IOException {
        when(mockedProperties.Get("org.freedesktop.NetworkManager.Device", "Autoconnect")).thenReturn(true);
        when(mockedProperties.Get("org.freedesktop.NetworkManager.Device", "FirmwareVersion"))
                .thenReturn("firmware");
        when(mockedProperties.Get("org.freedesktop.NetworkManager.Device", "Driver")).thenReturn("driver");
        when(mockedProperties.Get("org.freedesktop.NetworkManager.Device", "DriverVersion"))
                .thenReturn("1.0.0");
        when(mockedProperties.Get("org.freedesktop.NetworkManager.Device", "State"))
                .thenReturn(NMDeviceState.toUInt32(state));
        when(mockedProperties.Get("org.freedesktop.NetworkManager.Device", "Mtu")).thenReturn(new UInt32(100));
        when(mockedProperties.Get("org.freedesktop.NetworkManager.Device", "HwAddress"))
                .thenReturn("F5:5B:32:7C:40:EA");

        when(mockedProperties.Get("org.freedesktop.NetworkManager.Device", "Ip4Config")).thenReturn(new DBusPath("/"));
        when(mockedProperties.Get("org.freedesktop.NetworkManager.Device", "Ip6Config")).thenReturn(new DBusPath("/"));
    }

    private void givenModemMocksFor(String deviceId, String interfaceName, Properties mockedProperties, boolean hasBearers,
            boolean hasSims) throws DBusException {
        when(mockedProperties.Get("org.freedesktop.NetworkManager.Device.Modem", "DeviceId")).thenReturn("abcd1234");
        when(mockedProperties.Get("org.freedesktop.NetworkManager.Device", "IpInterface")).thenReturn("wwan0");
        when(mockedProperties.Get("org.freedesktop.NetworkManager.Device", "Udi")).thenReturn("/org/freedesktop/ModemManager1/Modem/3");

        Properties modemProperties = mock(Properties.class);
        doReturn(modemProperties).when(this.dbusConnection).getRemoteObject("org.freedesktop.ModemManager1",
                "/org/freedesktop/ModemManager1/Modem/3", Properties.class);
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "Device")).thenReturn(String.format("a/b/c/d/%s", deviceId));
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "Model")).thenReturn("AwesomeModel");
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "Manufacturer")).thenReturn("TheBestInTheWorld");
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "EquipmentIdentifier")).thenReturn("TheOne");
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "Revision")).thenReturn("1");
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "HardwareRevision")).thenReturn("S");
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "PrimaryPort")).thenReturn(interfaceName);
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "Ports"))
                .thenReturn(Arrays.asList(new Object[] { interfaceName, new UInt32(2) },
                        new Object[] { "ttyACM3", new UInt32(4) }));
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "SupportedCapabilities"))
                .thenReturn(Arrays.asList(new UInt32(4), new UInt32(8)));
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "CurrentCapabilities"))
                .thenReturn(new UInt32(0x0000000C));
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "PowerState"))
                .thenReturn(new UInt32(0x02));
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "SupportedModes"))
                .thenReturn(Arrays.asList(new Object[] { new UInt32(6), new UInt32(2) },
                        new Object[] { new UInt32(4), new UInt32(4) }));
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "CurrentModes"))
                .thenReturn(
                        new Object[] { new UInt32(6), new UInt32(2) });
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "SupportedBands"))
                .thenReturn(Arrays.asList(new UInt32[] { new UInt32(40), new UInt32(69), new UInt32(81) }));
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "CurrentBands"))
                .thenReturn(Arrays.asList(new UInt32[] { new UInt32(40), new UInt32(69) }));
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "PrimarySimSlot"))
                .thenReturn(new UInt32(0));
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "UnlockRequired"))
                .thenReturn(new UInt32(1));
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "State"))
                .thenReturn(8);
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "AccessTechnologies"))
                .thenReturn(new UInt32(0));
        when(modemProperties.Get(MM_MODEM_BUS_NAME, "SignalQuality"))
                .thenReturn(new UInt32[] { new UInt32(97), new UInt32(2) });
        when(modemProperties.Get("org.freedesktop.ModemManager1.Modem.Modem3gpp", "RegistrationState"))
                .thenReturn(new UInt32(5));
        when(modemProperties.Get("org.freedesktop.ModemManager1.Modem.Modem3gpp", "OperatorName"))
                .thenReturn("VeryCoolMobile");

        doThrow(new DBusExecutionException("Cannot find property")).when(modemProperties)
                .Get(MM_MODEM_BUS_NAME, "SimSlots");
        if (hasSims) {
            when(modemProperties.Get(MM_MODEM_BUS_NAME, "Sim"))
                    .thenReturn(new DBusPath("/org/freedesktop/ModemManager1/SIM/0"));

            Properties simProperties = mock(Properties.class);
            doReturn(simProperties).when(this.dbusConnection).getRemoteObject("org.freedesktop.ModemManager1",
                    "/org/freedesktop/ModemManager1/SIM/0", Properties.class);
            when(simProperties.Get("org.freedesktop.ModemManager1.Sim", "Active")).thenReturn(true);
            when(simProperties.Get("org.freedesktop.ModemManager1.Sim", "SimIdentifier"))
                    .thenReturn("VeryExpensiveSim");
            when(simProperties.Get("org.freedesktop.ModemManager1.Sim", "Imsi")).thenReturn("1234567890");
            doThrow(new DBusExecutionException("Cannot get eid property"))
                    .when(simProperties).Get("org.freedesktop.ModemManager1.Sim", "Eid");
            when(simProperties.Get("org.freedesktop.ModemManager1.Sim", "OperatorName")).thenReturn("VeryCoolMobile");
            when(simProperties.Get("org.freedesktop.ModemManager1.Sim", "SimType")).thenReturn(new UInt32(1));
            when(simProperties.Get("org.freedesktop.ModemManager1.Sim", "EsimStatus")).thenReturn(new UInt32(0));
        } else {
            when(modemProperties.Get(MM_MODEM_BUS_NAME, "Sim"))
                    .thenReturn(new DBusPath("/"));
        }

        // Modem location
        this.mockModemLocation = mock(Location.class);
        doReturn(this.mockModemLocation).when(this.dbusConnection).getRemoteObject("org.freedesktop.ModemManager1",
                "/org/freedesktop/ModemManager1/Modem/3", Location.class);
        doReturn("/org/freedesktop/ModemManager1/Modem/3").when(this.mockModemLocation).getObjectPath();


        Set<MMModemLocationSource> availableSources = EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_3GPP_LAC_CI, MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_RAW, MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_NMEA, MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_GPS_UNMANAGED);
        Set<MMModemLocationSource> enabledSources = EnumSet.of(MMModemLocationSource.MM_MODEM_LOCATION_SOURCE_3GPP_LAC_CI);
        when(modemProperties.Get("org.freedesktop.ModemManager1.Modem.Location", "Capabilities"))
                .thenReturn(MMModemLocationSource.toBitMaskFromMMModemLocationSource(availableSources));
        when(modemProperties.Get("org.freedesktop.ModemManager1.Modem.Location", "Enabled"))
                .thenReturn(MMModemLocationSource.toBitMaskFromMMModemLocationSource(enabledSources));
        // Modem location

        Modem modem = mock(Modem.class);
        doReturn(modem).when(this.dbusConnection).getRemoteObject("org.freedesktop.ModemManager1",
                "/org/freedesktop/ModemManager1/Modem/3", Modem.class);
        doThrow(new DBusExecutionException("Method not supported")).when(modem).ListBearers();
        if (hasBearers) {
            List<DBusPath> paths = Arrays
                    .asList(new DBusPath[] { new DBusPath("/org/freedesktop/ModemManager1/Bearer/0") });
            when(modemProperties.Get("org.freedesktop.ModemManager1", "Bearers")).thenReturn(paths);

            Properties bearerProperties = mock(Properties.class);
            doReturn(bearerProperties).when(this.dbusConnection).getRemoteObject("org.freedesktop.ModemManager1",
                    "/org/freedesktop/ModemManager1/Bearer/0", Properties.class);
            when(bearerProperties.Get("org.freedesktop.ModemManager1.Bearer", "Interface")).thenReturn(interfaceName);
            when(bearerProperties.Get("org.freedesktop.ModemManager1.Bearer", "Connected")).thenReturn(true);
            Map<String, Object> settings = new HashMap<>();
            settings.put("apn", "VeryCoolMobile.com");
            settings.put("ip-type", new UInt32(8));
            when(bearerProperties.Get("org.freedesktop.ModemManager1.Bearer", "Properties")).thenReturn(settings);
            Map<String, Object> stats = new HashMap<>();
            stats.put("tx-bytes", new UInt64(190));
            stats.put("rx-bytes", new UInt64(290));
            when(bearerProperties.Get("org.freedesktop.ModemManager1.Bearer", "Stats")).thenReturn(stats);
        } else {
            List<DBusPath> paths = Arrays.asList(new DBusPath[] { new DBusPath("/") });
            when(modemProperties.Get("org.freedesktop.ModemManager1", "Bearers")).thenReturn(paths);
        }
    }

    private void givenMockedDeviceList() {

        List<DBusPath> devicePaths = new ArrayList<>();

        for (String interfaceName : this.mockDevices.keySet()) {
            devicePaths.add(this.mockedNetworkManager.GetDeviceByIpIface(interfaceName));
        }
        when(this.mockedNetworkManager.GetAllDevices()).thenReturn(devicePaths);
    }

    private void givenNetworkConfigMapWith(String key, Object value) {
        this.netConfig.put(key, value);
    }

    private void givenApplyWasCalledOnceWith(Map<String, Object> networkConfig) throws DBusException {
        this.instanceNMDbusConnector.apply(networkConfig);
        clearInvocations(this.mockedNetworkManager);
        clearInvocations(this.dbusConnection);
        clearInvocations(this.mockConnection);
    }

    /*
     * When
     */

    private void whenGetDbusConnectionIsRun() {
        this.dbusConnectionInternal = this.instanceNMDbusConnector.getDbusConnection();
    }

    private void whenCheckPermissionsIsRun() {
        this.instanceNMDbusConnector.checkPermissions();
    }

    private void whenCheckVersionIsRun() {
        this.instanceNMDbusConnector.checkVersion();
    }

    private void whenGetInterfaceIdsIsCalled() {
        try {
            this.internalStringList = this.instanceNMDbusConnector.getInterfaceIds();
        } catch (DBusException e) {
            this.hasDBusExceptionBeenThrown = true;
        }
    }

    private void whenApplyIsCalled() {
        try {
            this.instanceNMDbusConnector.apply();
        } catch (DBusException e) {
            this.hasDBusExceptionBeenThrown = true;
        }
    }

    private void whenApplyIsCalledWith(Map<String, Object> networkConfig) {
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

    private void whenApplySingleIsCalledWith(String deviceId) {
        try {
            this.instanceNMDbusConnector.apply(deviceId);
        } catch (DBusException e) {
            this.hasDBusExceptionBeenThrown = true;
        } catch (NoSuchElementException e) {
            this.hasNoSuchElementExceptionThrown = true;
        } catch (NullPointerException e) {
            this.hasNullPointerExceptionThrown = true;
        } catch (IllegalArgumentException e) {
            this.hasIllegalArgumentExceptionThrown = true;
        }
    }

    private void whenGetInterfaceStatus(String netInterface, CommandExecutorService commandExecutorService) {
        try {
            this.netInterface = this.instanceNMDbusConnector.getInterfaceStatus(netInterface, false,
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

    private void whenGetInterfaceStatusWithRecompute(String netInterface,
            CommandExecutorService commandExecutorService) {
        try {
            this.netInterface = this.instanceNMDbusConnector.getInterfaceStatus(netInterface, true,
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

    private void thenNoExceptionIsThrown() {
        assertFalse(this.hasDBusExceptionBeenThrown);
        assertFalse(this.hasNoSuchElementExceptionThrown);
        assertFalse(this.hasNullPointerExceptionThrown);
        assertFalse(this.hasKuraExceptionThrown);
        assertFalse(this.hasIllegalArgumentExceptionThrown);
    }

    private void thenNullPointerExceptionIsThrown() {
        assertTrue(this.hasNullPointerExceptionThrown);
    }

    private void thenNoSuchElementExceptionIsThrown() {
        assertTrue(this.hasNoSuchElementExceptionThrown);
    }

    private void thenIllegalArgumentExceptionIsThrown() {
        assertTrue(this.hasIllegalArgumentExceptionThrown);
    }

    private void thenGetDbusConnectionReturns(DBusConnection dbusConnection) {
        assertEquals(this.dbusConnection, dbusConnection);
    }

    private void thenCheckVersionIsRun() throws DBusException, IOException {
        verify(this.dbusConnection, atLeastOnce()).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/org/freedesktop/NetworkManager"), any());
    }

    private void thenCheckPermissionsRan() {
        verify(this.mockedNetworkManager, atLeastOnce()).GetPermissions();
    }

    private void thenGetInterfacesReturn(List<String> list) {
        assertEquals(list, this.internalStringList);
    }

    private void thenDisconnectIsCalledFor(String netInterface) {
        verify(this.mockDevices.get(netInterface)).Disconnect();
    }

    private void thenConnectionUpdateIsCalledFor(String netInterface) throws DBusException {
        Connection connect = this.dbusConnection.getRemoteObject("org.freedesktop.NetworkManager",
                "/mock/device/" + netInterface, Connection.class);
        verify(connect).Update(any());
    }

    private void thenActivateConnectionIsCalledFor(String netInterface) throws DBusException {
        verify(this.mockedNetworkManager).ActivateConnection(any(), any(), any());
    }

    private void thenAddAndActivateConnectionIsCalledFor(String netInterface) throws DBusException {
        verify(this.mockedNetworkManagerSettings).AddConnection(any());
        verify(this.mockedNetworkManager).ActivateConnection(any(), any(), any());
    }
    
    private void thenAddConnectionIsCalledFor(String netInterface) throws DBusException {
        verify(this.mockedNetworkManagerSettings).AddConnection(any());
    }

    private void thenNetworkSettingsDidNotChangeForDevice(String netInterface) throws DBusException {
        verify(this.mockConnection, never()).Update(any());
        verify(this.mockDevices.get(netInterface), never()).Disconnect();
        verify(this.mockedNetworkManager, never()).ActivateConnection(any(), any(), any());
        verify(this.mockedNetworkManager, never()).AddAndActivateConnection(any(), any(), any());
    }

    private void thenInterfaceStatusIsNull() {
        assertNull(this.netInterface);
    }

    private void thenInterfaceStatusIsNotNull() {
        assertNotNull(this.netInterface);
    }

    private void thenNetInterfaceTypeIs(NetworkInterfaceType type) {
        assertEquals(type, this.netInterface.getType());
    }

    public void thenConfigurationEnforcementIsActive(boolean expectedValue) {
        assertEquals(expectedValue, this.instanceNMDbusConnector.configurationEnforcementIsActive());
    }

    private void thenConnectionIsDeleted(String path) {
        verify(this.mockedConnections.get(path), atLeastOnce()).Delete();
    }

    private void thenConnectionIsNotDeleted(String path) {
        verify(this.mockedConnections.get(path), times(0)).Delete();
    }

    private void thenLocationSetupWasCalledWith(EnumSet<MMModemLocationSource> expectedLocationSources,
            boolean expectedFlag) {
        verify(this.mockModemLocation, times(1))
                .Setup(MMModemLocationSource.toBitMaskFromMMModemLocationSource(expectedLocationSources), expectedFlag);
    }

    private void thenScanIsTriggered(String interfaceId) {
        verify(this.mockedInterfaces.get(interfaceId), times(1)).Scan(any());
    }

    private void thenModemStatusHasCorrectValues(boolean hasBearers, boolean hasSims) {
        assertTrue(this.netInterface instanceof ModemInterfaceStatus);
        ModemInterfaceStatus modemStatus = (ModemInterfaceStatus) this.netInterface;
        assertEquals("1-5", modemStatus.getInterfaceId());
        assertEquals("wwan0", modemStatus.getInterfaceName());
        assertEquals("AwesomeModel", modemStatus.getModel());
        assertEquals("TheBestInTheWorld", modemStatus.getManufacturer());
        assertEquals("TheOne", modemStatus.getSerialNumber());
        assertEquals("1", modemStatus.getSoftwareRevision());
        assertEquals("S", modemStatus.getHardwareRevision());
        assertEquals("ttyACM17", modemStatus.getPrimaryPort());
        assertEquals(2, modemStatus.getPorts().size());
        assertEquals(ModemPortType.NET, modemStatus.getPorts().get("ttyACM17"));
        assertEquals(ModemPortType.QCDM, modemStatus.getPorts().get("ttyACM3"));
        assertEquals(2, modemStatus.getSupportedModemCapabilities().size());
        assertTrue(modemStatus.getSupportedModemCapabilities().contains(ModemCapability.GSM_UMTS));
        assertTrue(modemStatus.getSupportedModemCapabilities().contains(ModemCapability.LTE));
        assertEquals(2, modemStatus.getCurrentModemCapabilities().size());
        assertTrue(modemStatus.getCurrentModemCapabilities().contains(ModemCapability.GSM_UMTS));
        assertTrue(modemStatus.getCurrentModemCapabilities().contains(ModemCapability.LTE));
        assertEquals(ModemPowerState.LOW, modemStatus.getPowerState());
        assertEquals(2, modemStatus.getSupportedModes().size());
        assertTrue(modemStatus.getSupportedModes()
                .contains(new ModemModePair(EnumSet.of(ModemMode.MODE_2G, ModemMode.MODE_3G), ModemMode.MODE_2G)));
        assertTrue(modemStatus.getSupportedModes()
                .contains(new ModemModePair(EnumSet.of(ModemMode.MODE_3G), ModemMode.MODE_3G)));
        assertEquals(3, modemStatus.getSupportedBands().size());
        assertTrue(modemStatus.getSupportedBands().contains(ModemBand.EUTRAN_10));
        assertTrue(modemStatus.getSupportedBands().contains(ModemBand.EUTRAN_39));
        assertTrue(modemStatus.getSupportedBands().contains(ModemBand.EUTRAN_51));
        assertEquals(2, modemStatus.getCurrentBands().size());
        assertTrue(modemStatus.getCurrentBands().contains(ModemBand.EUTRAN_10));
        assertTrue(modemStatus.getCurrentBands().contains(ModemBand.EUTRAN_39));
        assertTrue(modemStatus.isGpsSupported());
        assertFalse(modemStatus.isSimLocked());
        assertEquals(ModemConnectionStatus.REGISTERED, modemStatus.getConnectionStatus());
        assertEquals(1, modemStatus.getAccessTechnologies().size());
        assertTrue(modemStatus.getAccessTechnologies().contains(AccessTechnology.UNKNOWN));
        assertEquals(97, modemStatus.getSignalQuality());
        assertEquals(-55, modemStatus.getSignalStrength());
        assertEquals(RegistrationStatus.ROAMING, modemStatus.getRegistrationStatus());
        assertEquals("VeryCoolMobile", modemStatus.getOperatorName());
        if (hasSims) {
            assertEquals(1, modemStatus.getAvailableSims().size());
            Sim sim = modemStatus.getAvailableSims().get(0);
            assertTrue(sim.isActive());
            assertEquals("VeryExpensiveSim", sim.getIccid());
            assertEquals("1234567890", sim.getImsi());
            assertTrue(sim.getEid().isEmpty());
            assertEquals("VeryCoolMobile", sim.getOperatorName());
            assertEquals(SimType.PHYSICAL, sim.getSimType());
            assertEquals(ESimStatus.UNKNOWN, sim.geteSimStatus());
        } else {
            assertTrue(modemStatus.getAvailableSims().isEmpty());
        }
        if (hasBearers) {
            assertEquals(1, modemStatus.getBearers().size());
            Bearer bearer = modemStatus.getBearers().get(0);
            assertEquals("ttyACM17", bearer.getName());
            assertTrue(bearer.isConnected());
            assertEquals("VeryCoolMobile.com", bearer.getApn());
            assertTrue(bearer.getIpTypes().contains(BearerIpType.NON_IP));
            assertEquals(190, bearer.getBytesTransmitted());
            assertEquals(290, bearer.getBytesReceived());
        } else {
            assertTrue(modemStatus.getBearers().isEmpty());
        }
    }
    
    private void thenDeviceExists(String interfaceName) {
        assertTrue(this.mockDevices.containsKey(interfaceName));
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

        when(preMockedProperties.Get("org.freedesktop.NetworkManager.Device.Wireless", "ActiveAccessPoint"))
                .thenReturn(mockedApPath);
        when(preMockedProperties.Get("org.freedesktop.NetworkManager.Device.Wireless", "Mode"))
                .thenReturn(new UInt32(1));
        when(preMockedProperties.Get("org.freedesktop.NetworkManager.Device.Wireless", "WirelessCapabilities"))
                .thenReturn(new UInt32(1));

        doReturn(wirelessDevice).when(this.dbusConnection).getRemoteObject("org.freedesktop.NetworkManager",
                "/mock/device/" + interfaceName, Wireless.class);
    }

}
