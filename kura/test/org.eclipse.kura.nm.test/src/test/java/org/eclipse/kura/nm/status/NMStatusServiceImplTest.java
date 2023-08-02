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
package org.eclipse.kura.nm.status;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.modem.ModemConnectionType;
import org.eclipse.kura.net.status.NetworkInterfaceIpAddress;
import org.eclipse.kura.net.status.NetworkInterfaceIpAddressStatus;
import org.eclipse.kura.net.status.NetworkInterfaceState;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.NetworkInterfaceStatus.NetworkInterfaceStatusBuilder;
import org.eclipse.kura.net.status.NetworkInterfaceType;
import org.eclipse.kura.net.status.ethernet.EthernetInterfaceStatus;
import org.eclipse.kura.net.status.ethernet.EthernetInterfaceStatus.EthernetInterfaceStatusBuilder;
import org.eclipse.kura.net.status.loopback.LoopbackInterfaceStatus;
import org.eclipse.kura.net.status.loopback.LoopbackInterfaceStatus.LoopbackInterfaceStatusBuilder;
import org.eclipse.kura.net.status.modem.AccessTechnology;
import org.eclipse.kura.net.status.modem.Bearer;
import org.eclipse.kura.net.status.modem.BearerIpType;
import org.eclipse.kura.net.status.modem.ESimStatus;
import org.eclipse.kura.net.status.modem.ModemBand;
import org.eclipse.kura.net.status.modem.ModemCapability;
import org.eclipse.kura.net.status.modem.ModemConnectionStatus;
import org.eclipse.kura.net.status.modem.ModemInterfaceStatus;
import org.eclipse.kura.net.status.modem.ModemInterfaceStatus.ModemInterfaceStatusBuilder;
import org.eclipse.kura.net.status.modem.ModemMode;
import org.eclipse.kura.net.status.modem.ModemModePair;
import org.eclipse.kura.net.status.modem.ModemPortType;
import org.eclipse.kura.net.status.modem.ModemPowerState;
import org.eclipse.kura.net.status.modem.RegistrationStatus;
import org.eclipse.kura.net.status.modem.Sim;
import org.eclipse.kura.net.status.modem.SimType;
import org.eclipse.kura.net.status.wifi.WifiAccessPoint;
import org.eclipse.kura.net.status.wifi.WifiAccessPoint.WifiAccessPointBuilder;
import org.eclipse.kura.net.status.wifi.WifiCapability;
import org.eclipse.kura.net.status.wifi.WifiChannel;
import org.eclipse.kura.net.status.wifi.WifiInterfaceStatus;
import org.eclipse.kura.net.status.wifi.WifiInterfaceStatus.WifiInterfaceStatusBuilder;
import org.eclipse.kura.net.status.wifi.WifiMode;
import org.eclipse.kura.net.status.wifi.WifiSecurity;
import org.eclipse.kura.nm.NMDbusConnector;
import org.freedesktop.dbus.errors.UnknownMethod;
import org.freedesktop.dbus.exceptions.DBusException;
import org.junit.Test;

public class NMStatusServiceImplTest {

    private NMStatusServiceImpl statusService;
    private NMDbusConnector nmDbusConnector;
    private CommandExecutorService commandExecutorService;
    private Optional<NetworkInterfaceStatus> status;
    private List<String> interfaceNames = new ArrayList<>();
    private boolean exceptionCaught = false;

    @Test
    public void shouldReturnEmptyStatusIfInterfaceDoesNotExist()
            throws DBusException, UnknownHostException, KuraException {
        givenNMStatusServiceImplWithNonExistingInterface();
        whenInterfaceStatusIsRetrieved("non-existing-interface");
        thenInterfaceStatusIsEmpty();
    }

    @Test
    public void shouldThrowKuraExceptionOnStatusRetrivingIfDBusObjectVanishes() throws DBusException, KuraException {
        givenNMStatusServiceImplThrowingUnknownMethodOnGetInterfaceStatus();
        whenInterfaceStatusIsRetrieved("wlan1");
        thenKuraExceptionIsCaught();
    }

    @Test
    public void shouldThrowKuraExceptionOnStatusRetrivingIfErrorOccours() throws DBusException, KuraException {
        givenNMStatusServiceImplThrowingDBusExceptionOnGetInterfaceStatus();
        whenInterfaceStatusIsRetrieved("wlan1");
        thenKuraExceptionIsCaught();
    }

    @Test
    public void shouldReturnExistingEthernetInterfaceStatus()
            throws DBusException, UnknownHostException, KuraException {
        givenNMStatusServiceImplWithEthernetInterface();
        whenInterfaceStatusIsRetrieved("abcd0");
        thenInterfaceStatusIsReturned();
    }

    @Test
    public void shouldReturnFullEthernetInterfaceStatus() throws UnknownHostException, DBusException, KuraException {
        givenNMStatusServiceImplWithEthernetInterface();
        whenInterfaceStatusIsRetrieved("abcd0");
        thenInterfaceStatusIsReturned();
        thenEthernetInterfaceStatusIsRetrieved();
        thenRetrievedEthernetInterfaceStatusHasFullProperties();
    }

    @Test
    public void shouldReturnExistingWiFiInterfaceStatus() throws DBusException, UnknownHostException, KuraException {
        givenNMStatusServiceImplWithWifiInterface();
        whenInterfaceStatusIsRetrieved("wlan0");
        thenInterfaceStatusIsReturned();
    }

    @Test
    public void shouldReturnFullWifiInterfaceStatus() throws DBusException, UnknownHostException, KuraException {
        givenNMStatusServiceImplWithWifiInterface();
        whenInterfaceStatusIsRetrieved("wlan0");
        thenInterfaceStatusIsReturned();
        thenWifiInterfaceStatusIsRetrieved();
        thenRetrievedWifiInterfaceStatusHasFullProperties();
    }

    @Test
    public void shouldReturnEmptyInterfaceNameListIfNoInterfaces()
            throws UnknownHostException, DBusException, KuraException {
        givenNMStatusServiceImplWithoutInterfaces();
        whenInterfaceNameListIsRetrived();
        thenInterfaceNameListIsEmpty();
    }

    @Test
    public void shouldReturnInterfaceNameList() throws UnknownHostException, DBusException, KuraException {
        givenNMStatusServiceImplWithInterfaces();
        whenInterfaceNameListIsRetrived();
        thenInterfaceNameListIsNotEmpty();
    }

    @Test
    public void shouldThrowKuraExceptionIfInterfaceNameListCannotBeRetrieved()
            throws UnknownHostException, DBusException, KuraException {
        givenNMStatusServiceImplThrowingDBusExceptionOnGetInterfaces();
        whenInterfaceNameListIsRetrived();
        thenKuraExceptionIsCaught();
    }

    @Test
    public void shouldReturnExistingLoopbackInterfaceStatus()
            throws DBusException, UnknownHostException, KuraException {
        givenNMStatusServiceImplWithLoopbackInterface();
        whenInterfaceStatusIsRetrieved("lo");
        thenInterfaceStatusIsReturned();
    }

    @Test
    public void shouldReturnFullLoopbackInterfaceStatus() throws DBusException, UnknownHostException, KuraException {
        givenNMStatusServiceImplWithLoopbackInterface();
        whenInterfaceStatusIsRetrieved("lo");
        thenInterfaceStatusIsReturned();
        thenLoopbackInterfaceStatusIsRetrieved();
        thenRetrievedLoopbackInterfaceStatusHasFullProperties();
    }

    @Test
    public void shouldReturnExistingModemInterfaceStatus() throws UnknownHostException, DBusException, KuraException {
        givenNMStatusServiceImplWithModemInterface();
        whenInterfaceStatusIsRetrieved("wwan0");
        thenInterfaceStatusIsReturned();
    }

    @Test
    public void shouldReturnFullModemInterfaceStatus() throws DBusException, UnknownHostException, KuraException {
        givenNMStatusServiceImplWithModemInterface();
        whenInterfaceStatusIsRetrieved("wwan0");
        thenInterfaceStatusIsReturned();
        thenModemInterfaceStatusIsRetrieved();
        thenRetrievedModemkInterfaceStatusHasFullProperties();
    }

    private void givenNMStatusServiceImplWithNonExistingInterface()
            throws DBusException, UnknownHostException, KuraException {
        createTestObjects();
        when(this.nmDbusConnector.getInterfaceStatus("non-existing-interface", false, this.commandExecutorService))
                .thenReturn(null);
    }

    private void givenNMStatusServiceImplThrowingUnknownMethodOnGetInterfaceStatus()
            throws DBusException, KuraException {
        createTestObjects();
        when(this.nmDbusConnector.getInterfaceStatus("wlan1", false, this.commandExecutorService))
                .thenThrow(UnknownMethod.class);
    }

    private void givenNMStatusServiceImplThrowingDBusExceptionOnGetInterfaceStatus()
            throws DBusException, KuraException {
        createTestObjects();
        when(this.nmDbusConnector.getInterfaceStatus("wlan1", false, this.commandExecutorService))
                .thenThrow(DBusException.class);
    }

    private void givenNMStatusServiceImplWithEthernetInterface()
            throws DBusException, UnknownHostException, KuraException {
        createTestObjects();
        when(this.nmDbusConnector.getInterfaceStatus("abcd0", false, this.commandExecutorService))
                .thenReturn(buildEthernetInterfaceStatus("abcd0"));
    }

    private void givenNMStatusServiceImplWithWifiInterface() throws DBusException, UnknownHostException, KuraException {
        createTestObjects();
        when(this.nmDbusConnector.getInterfaceStatus("wlan0", false, this.commandExecutorService))
                .thenReturn(buildWifiInterfaceStatus("wlan0"));
    }

    private void givenNMStatusServiceImplWithoutInterfaces() throws DBusException, UnknownHostException {
        createTestObjects();
        when(this.nmDbusConnector.getInterfaceIds()).thenReturn(Collections.emptyList());
    }

    private void givenNMStatusServiceImplWithInterfaces() throws DBusException, UnknownHostException, KuraException {
        createTestObjects();
        when(this.nmDbusConnector.getInterfaceIds()).thenReturn(Arrays.asList("abcd0", "wlan0"));
        when(this.nmDbusConnector.getInterfaceStatus("abcd0", false, this.commandExecutorService))
                .thenReturn(buildEthernetInterfaceStatus("abcd0"));
        when(this.nmDbusConnector.getInterfaceStatus("wlan0", false, this.commandExecutorService))
                .thenReturn(buildWifiInterfaceStatus("wlan0"));
        when(this.nmDbusConnector.getInterfaceStatus("broken-interface", false, this.commandExecutorService))
                .thenThrow(new DBusException("Cannot retrieve interface."));
    }

    private void givenNMStatusServiceImplWithLoopbackInterface()
            throws UnknownHostException, DBusException, KuraException {
        createTestObjects();
        when(this.nmDbusConnector.getInterfaceStatus("lo", false, this.commandExecutorService))
                .thenReturn(buildLoopbackInterfaceStatus("lo"));
    }

    private void givenNMStatusServiceImplWithModemInterface()
            throws UnknownHostException, DBusException, KuraException {
        createTestObjects();
        when(this.nmDbusConnector.getInterfaceStatus("wwan0", false, this.commandExecutorService))
                .thenReturn(buildModemInterfaceStatus("wwan0"));
    }

    private void givenNMStatusServiceImplThrowingDBusExceptionOnGetInterfaces()
            throws UnknownHostException, DBusException, KuraException {
        createTestObjects();
        when(this.nmDbusConnector.getInterfaceIds()).thenThrow(new DBusException("Cannot retrieve interface list."));
    }

    private void whenInterfaceStatusIsRetrieved(String interfaceName) {
        try {
            this.status = this.statusService.getNetworkStatus(interfaceName);
        } catch (KuraException e) {
            this.exceptionCaught = true;
        }
    }

    private void whenInterfaceNameListIsRetrived() {
        try {
            this.interfaceNames = this.statusService.getInterfaceIds();
        } catch (KuraException e) {
            this.exceptionCaught = true;
        }
    }

    private void thenInterfaceStatusIsEmpty() {
        assertNotNull(this.status);
        assertFalse(this.status.isPresent());
    }

    private void thenInterfaceStatusIsReturned() {
        assertNotNull(this.status);
        assertTrue(this.status.isPresent());
    }

    private void thenEthernetInterfaceStatusIsRetrieved() {
        assertTrue(this.status.get() instanceof EthernetInterfaceStatus);
        assertEquals(NetworkInterfaceType.ETHERNET, this.status.get().getType());
    }

    private void thenRetrievedEthernetInterfaceStatusHasFullProperties() throws UnknownHostException {
        EthernetInterfaceStatus ethStatus = (EthernetInterfaceStatus) this.status.get();
        assertEquals("abcd0", ethStatus.getInterfaceId());
        assertCommonProperties(ethStatus);
        assertTrue(ethStatus.isLinkUp());
        assertEquals(buildEthernetInterfaceStatus("abcd0"), ethStatus);
        assertEquals(buildEthernetInterfaceStatus("abcd0").hashCode(), ethStatus.hashCode());
    }

    private void thenWifiInterfaceStatusIsRetrieved() {
        assertTrue(this.status.get() instanceof WifiInterfaceStatus);
        assertEquals(NetworkInterfaceType.WIFI, this.status.get().getType());
    }

    private void thenRetrievedWifiInterfaceStatusHasFullProperties() throws UnknownHostException {
        WifiInterfaceStatus wifiStatus = (WifiInterfaceStatus) this.status.get();
        assertEquals("wlan0", wifiStatus.getInterfaceId());
        assertCommonProperties(wifiStatus);
        assertEquals(2, wifiStatus.getCapabilities().size());
        assertEquals(EnumSet.of(WifiCapability.AP, WifiCapability.FREQ_2GHZ), wifiStatus.getCapabilities());
        assertEquals(4, wifiStatus.getChannels().size());
        assertEquals(
                Arrays.asList(WifiChannel.builder(1, 2412).build(), WifiChannel.builder(2, 2417).build(),
                        WifiChannel.builder(3, 2422).build(), WifiChannel.builder(4, 2432).build()),
                wifiStatus.getChannels());
        assertEquals("IT", wifiStatus.getCountryCode());
        assertEquals(WifiMode.INFRA, wifiStatus.getMode());
        assertTrue(wifiStatus.getActiveWifiAccessPoint().isPresent());
        assertEquals(buildAP(), wifiStatus.getActiveWifiAccessPoint().get());
        assertEqualAPProperties(wifiStatus.getActiveWifiAccessPoint().get());
        assertEquals(1, wifiStatus.getAvailableWifiAccessPoints().size());
        assertEquals(buildAP(), wifiStatus.getAvailableWifiAccessPoints().get(0));
        assertEquals(buildWifiInterfaceStatus("wlan0"), wifiStatus);
        assertEquals(buildWifiInterfaceStatus("wlan0").hashCode(), wifiStatus.hashCode());
    }

    private void thenInterfaceNameListIsEmpty() {
        assertNotNull(this.interfaceNames);
        assertTrue(this.interfaceNames.isEmpty());
    }

    private void thenInterfaceNameListIsNotEmpty() {
        assertNotNull(this.interfaceNames);
        assertFalse(this.interfaceNames.isEmpty());
        assertEquals(2, this.interfaceNames.size());
    }

    private void thenLoopbackInterfaceStatusIsRetrieved() {
        assertTrue(this.status.get() instanceof LoopbackInterfaceStatus);
        assertEquals(NetworkInterfaceType.LOOPBACK, this.status.get().getType());
    }

    private void thenModemInterfaceStatusIsRetrieved() {
        assertTrue(this.status.get() instanceof ModemInterfaceStatus);
        assertEquals(NetworkInterfaceType.MODEM, this.status.get().getType());
    }

    private void thenRetrievedLoopbackInterfaceStatusHasFullProperties() throws UnknownHostException {
        LoopbackInterfaceStatus loStatus = (LoopbackInterfaceStatus) this.status.get();
        assertEquals("lo", loStatus.getInterfaceId());
        assertTrue(Arrays.equals(new byte[] { 0x00, 0x11, 0x02, 0x33, 0x44, 0x55 }, loStatus.getHardwareAddress()));
        assertEquals("EthDriver", loStatus.getDriver());
        assertEquals("EthDriverVersion", loStatus.getDriverVersion());
        assertEquals("1234", loStatus.getFirmwareVersion());
        assertTrue(loStatus.isVirtual());
        assertEquals(NetworkInterfaceState.ACTIVATED, loStatus.getState());
        assertTrue(loStatus.isAutoConnect());
        assertEquals(1500, loStatus.getMtu());
        assertTrue(loStatus.getInterfaceIp4Addresses().isPresent());
        assertEquals(buildIp4Address(), loStatus.getInterfaceIp4Addresses().get());
        assertTrue(loStatus.getInterfaceIp6Addresses().isPresent());
        assertEquals(buildIp6Address(), loStatus.getInterfaceIp6Addresses().get());
        assertEquals(buildLoopbackInterfaceStatus("lo"), loStatus);
        assertEquals(buildLoopbackInterfaceStatus("lo").hashCode(), loStatus.hashCode());
    }

    private void thenRetrievedModemkInterfaceStatusHasFullProperties() throws UnknownHostException {
        ModemInterfaceStatus modemStatus = (ModemInterfaceStatus) this.status.get();
        assertEquals("wwan0", modemStatus.getInterfaceId());
        assertTrue(Arrays.equals(new byte[] { 0x00, 0x11, 0x02, 0x33, 0x44, 0x55 }, modemStatus.getHardwareAddress()));
        assertEquals("EthDriver", modemStatus.getDriver());
        assertEquals("EthDriverVersion", modemStatus.getDriverVersion());
        assertEquals("1234", modemStatus.getFirmwareVersion());
        assertFalse(modemStatus.isVirtual());
        assertEquals(NetworkInterfaceState.ACTIVATED, modemStatus.getState());
        assertTrue(modemStatus.isAutoConnect());
        assertEquals(1500, modemStatus.getMtu());
        assertTrue(modemStatus.getInterfaceIp4Addresses().isPresent());
        assertEquals(buildIp4Address(), modemStatus.getInterfaceIp4Addresses().get());
        assertTrue(modemStatus.getInterfaceIp6Addresses().isPresent());
        assertEquals(buildIp6Address(), modemStatus.getInterfaceIp6Addresses().get());
        assertEquals(buildModemInterfaceStatus("wwan0"), modemStatus);
        assertEquals(buildModemInterfaceStatus("wwan0").hashCode(), modemStatus.hashCode());
    }

    private void thenKuraExceptionIsCaught() {
        assertTrue(this.exceptionCaught);
    }

    private void assertCommonProperties(NetworkInterfaceStatus networkStatus) throws UnknownHostException {
        assertTrue(
                Arrays.equals(new byte[] { 0x00, 0x11, 0x02, 0x33, 0x44, 0x55 }, networkStatus.getHardwareAddress()));
        assertEquals("EthDriver", networkStatus.getDriver());
        assertEquals("EthDriverVersion", networkStatus.getDriverVersion());
        assertEquals("1234", networkStatus.getFirmwareVersion());
        assertFalse(networkStatus.isVirtual());
        assertEquals(NetworkInterfaceState.ACTIVATED, networkStatus.getState());
        assertTrue(networkStatus.isAutoConnect());
        assertEquals(1500, networkStatus.getMtu());
        assertTrue(networkStatus.getInterfaceIp4Addresses().isPresent());
        assertEquals(buildIp4Address(), networkStatus.getInterfaceIp4Addresses().get());
        assertEqualsIp4AddressStatus(networkStatus.getInterfaceIp4Addresses().get());
        assertTrue(networkStatus.getInterfaceIp6Addresses().isPresent());
        assertEquals(buildIp6Address(), networkStatus.getInterfaceIp6Addresses().get());
        assertEqualsIp6AddressStatus(networkStatus.getInterfaceIp6Addresses().get());
    }

    private EthernetInterfaceStatus buildEthernetInterfaceStatus(String interfaceName) throws UnknownHostException {
        EthernetInterfaceStatusBuilder builder = EthernetInterfaceStatus.builder();
        buildCommonProperties(interfaceName, builder);
        builder.withIsLinkUp(true);
        return builder.build();
    }

    private WifiInterfaceStatus buildWifiInterfaceStatus(String interfaceName) throws UnknownHostException {
        WifiInterfaceStatusBuilder builder = WifiInterfaceStatus.builder();
        buildCommonProperties(interfaceName, builder);
        builder.withCapabilities(EnumSet.of(WifiCapability.AP, WifiCapability.FREQ_2GHZ));
        builder.withWifiChannels(
                Arrays.asList(WifiChannel.builder(1, 2412).build(), WifiChannel.builder(2, 2417).build(),
                        WifiChannel.builder(3, 2422).build(), WifiChannel.builder(4, 2432).build()));
        builder.withCountryCode("IT");
        builder.withMode(WifiMode.INFRA);
        builder.withActiveWifiAccessPoint(Optional.of(buildAP()));
        builder.withAvailableWifiAccessPoints(Arrays.asList(buildAP()));
        return builder.build();
    }

    private LoopbackInterfaceStatus buildLoopbackInterfaceStatus(String interfaceName) throws UnknownHostException {
        LoopbackInterfaceStatusBuilder builder = LoopbackInterfaceStatus.builder();
        buildCommonProperties(interfaceName, builder);
        builder.withVirtual(true);
        return builder.build();
    }

    private ModemInterfaceStatus buildModemInterfaceStatus(String interfaceName) throws UnknownHostException {
        ModemInterfaceStatusBuilder builder = ModemInterfaceStatus.builder();
        buildCommonProperties(interfaceName, builder);
        builder.withVirtual(false);
        builder.withModel("CoolModel");
        builder.withManufacturer("CoolManufacturer");
        builder.withSerialNumber("1234AB");
        builder.withSoftwareRevision("TheBestOne");
        builder.withHardwareRevision("NaN");
        builder.withPrimaryPort("Port1");
        builder.withPorts(getPorts());
        builder.withSupportedModemCapabilities(getSupportedModemCapabilities());
        builder.withCurrentModemCapabilities(getCurrentModemCapabilities());
        builder.withPowerState(ModemPowerState.LOW);
        builder.withSupportedModes(getSupportedModemModes());
        builder.withCurrentModes(
                new ModemModePair(EnumSet.of(ModemMode.MODE_2G, ModemMode.MODE_3G), ModemMode.MODE_3G));
        builder.withSupportedBands(getSupportedModemBands());
        builder.withCurrentBands(getCurrentModemBands());
        builder.withGpsSupported(false);
        builder.withAvailableSims(getAvailableSims());
        builder.withSimLocked(false);
        builder.withBearers(getBearers());
        builder.withConnectionType(ModemConnectionType.DirectIP);
        builder.withConnectionStatus(ModemConnectionStatus.REGISTERED);
        builder.withAccessTechnologies(getAccessTechnologies());
        builder.withSignalQuality(100);
        builder.withSignalQuality(-53);
        builder.withRegistrationStatus(RegistrationStatus.EMERGENCY_ONLY);
        builder.withOperatorName("WhoCares?");

        return builder.build();
    }

    private Map<String, ModemPortType> getPorts() {
        Map<String, ModemPortType> ports = new HashMap<>();
        ports.put("Port1", ModemPortType.AT);
        ports.put("Port2", ModemPortType.NET);
        ports.put("Port3", ModemPortType.QMI);
        ports.put("Port4", ModemPortType.UNKNOWN);
        return ports;
    }

    private Set<ModemCapability> getSupportedModemCapabilities() {
        return EnumSet.of(ModemCapability.GSM_UMTS, ModemCapability.LTE, ModemCapability.IRIDIUM);
    }

    private Set<ModemCapability> getCurrentModemCapabilities() {
        return EnumSet.noneOf(ModemCapability.class);
    }

    private Set<ModemModePair> getSupportedModemModes() {
        Set<ModemModePair> modes = new HashSet<>();
        modes.add(new ModemModePair(EnumSet.of(ModemMode.CS, ModemMode.MODE_5G), ModemMode.MODE_5G));
        modes.add(new ModemModePair(EnumSet.of(ModemMode.MODE_2G, ModemMode.MODE_3G), ModemMode.MODE_3G));
        return modes;
    }

    private Set<ModemBand> getSupportedModemBands() {
        return EnumSet.of(ModemBand.CDMA_BC15, ModemBand.EUTRAN_2, ModemBand.NGRAN_1);
    }

    private Set<ModemBand> getCurrentModemBands() {
        return EnumSet.of(ModemBand.NGRAN_1);
    }

    private List<Sim> getAvailableSims() {
        List<Sim> sims = new ArrayList<>();
        sims.add(Sim.builder().withActive(false).withPrimary(false).withIccid("1234").withImsi("5678").withEid("90")
                .withOperatorName("VeryCoolMobile").withSimType(SimType.PHYSICAL).withESimStatus(ESimStatus.UNKNOWN)
                .build());
        sims.add(Sim.builder().withActive(true).withPrimary(true).withIccid("ABCD").withImsi("DEFG").withEid("HI")
                .withOperatorName("UglyMobile").withSimType(SimType.PHYSICAL).withESimStatus(ESimStatus.UNKNOWN)
                .build());
        return sims;
    }

    private List<Bearer> getBearers() {
        List<Bearer> bearers = new ArrayList<>();
        bearers.add(new Bearer("Bearer1", false, "web.verycoolmobile.com", EnumSet.of(BearerIpType.IPV4V6), 0, 0));
        bearers.add(new Bearer("Bearer2", false, "web.uglymobile.com", EnumSet.of(BearerIpType.IPV4), 0, 0));
        return bearers;
    }

    private Set<AccessTechnology> getAccessTechnologies() {
        return EnumSet.of(AccessTechnology.EDGE, AccessTechnology.LTE_NB_IOT);
    }

    private void buildCommonProperties(String interfaceName, NetworkInterfaceStatusBuilder<?> builder)
            throws UnknownHostException {
        builder.withInterfaceId(interfaceName);
        builder.withHardwareAddress(new byte[] { 0x00, 0x11, 0x02, 0x33, 0x44, 0x55 });
        builder.withDriver("EthDriver").withDriverVersion("EthDriverVersion").withFirmwareVersion("1234");
        builder.withVirtual(false).withState(NetworkInterfaceState.ACTIVATED).withAutoConnect(true).withMtu(1500);
        builder.withInterfaceIp4Addresses(Optional.of(buildIp4Address()));
        builder.withInterfaceIp6Addresses(Optional.of(buildIp6Address()));
    }

    private NetworkInterfaceIpAddressStatus<IP4Address> buildIp4Address() throws UnknownHostException {
        NetworkInterfaceIpAddress<IP4Address> ip4Address = new NetworkInterfaceIpAddress<>(
                (IP4Address) IPAddress.parseHostAddress("172.16.2.100"), (short) 16);
        NetworkInterfaceIpAddressStatus.Builder<IP4Address> builder = NetworkInterfaceIpAddressStatus.builder();
        builder.withAddresses(Collections.singletonList(ip4Address));
        builder.withGateway(Optional.of((IP4Address) IPAddress.parseHostAddress("172.16.2.1")));
        builder.withDnsServerAddresses(
                Collections.singletonList((IP4Address) IPAddress.parseHostAddress("172.16.2.23")));

        return builder.build();
    }

    private NetworkInterfaceIpAddressStatus<IP6Address> buildIp6Address() throws UnknownHostException {
        NetworkInterfaceIpAddress<IP6Address> ip6Address = new NetworkInterfaceIpAddress<>(
                (IP6Address) IPAddress.parseHostAddress("2345:425:2CA1:0000:0000:567:5673:23b5"), (short) 64);

        NetworkInterfaceIpAddressStatus.Builder<IP6Address> builder = NetworkInterfaceIpAddressStatus.builder();
        builder.withAddresses(Collections.singletonList(ip6Address));
        builder.withGateway(
                Optional.of((IP6Address) IPAddress.parseHostAddress("2345:425:2CA1:0000:0000:567:5673:23b6")));
        builder.withDnsServerAddresses(Collections
                .singletonList((IP6Address) IPAddress.parseHostAddress("2345:425:2CA1:0000:0000:567:5673:23b7")));

        return builder.build();
    }

    private WifiAccessPoint buildAP() {
        WifiAccessPointBuilder builder = WifiAccessPoint.builder();
        builder.withSsid("MyCoolAP");
        builder.withHardwareAddress(new byte[] { 0x00, 0x11, 0x02, 0x33, 0x44, 0x55 });
        builder.withChannel(WifiChannel.builder(7, 2442).build());
        builder.withMode(WifiMode.INFRA);
        builder.withMaxBitrate(54);
        builder.withSignalQuality(78);
        builder.withWpaSecurity(EnumSet.of(WifiSecurity.GROUP_CCMP, WifiSecurity.GROUP_TKIP));
        builder.withRsnSecurity(EnumSet.of(WifiSecurity.GROUP_WEP104, WifiSecurity.KEY_MGMT_802_1X));
        return builder.build();
    }

    private void assertEqualAPProperties(WifiAccessPoint ap) {
        assertEquals("MyCoolAP", ap.getSsid());
        assertArrayEquals(new byte[] { 0x00, 0x11, 0x02, 0x33, 0x44, 0x55 }, ap.getHardwareAddress());
        assertEquals(WifiChannel.builder(7, 2442).build(), ap.getChannel());
        assertEquals(WifiMode.INFRA, ap.getMode());
        assertEquals(54, ap.getMaxBitrate());
        assertEquals(78, ap.getSignalQuality());
        assertEquals(2, ap.getWpaSecurity().size());
        assertEquals(EnumSet.of(WifiSecurity.GROUP_CCMP, WifiSecurity.GROUP_TKIP), ap.getWpaSecurity());
        assertEquals(EnumSet.of(WifiSecurity.GROUP_WEP104, WifiSecurity.KEY_MGMT_802_1X), ap.getRsnSecurity());
        assertEquals(2, ap.getRsnSecurity().size());
    }

    private void assertEqualsIp4AddressStatus(NetworkInterfaceIpAddressStatus<IP4Address> ip4AddressStatus)
            throws UnknownHostException {
        assertInterfaceIp4AddressEquals(ip4AddressStatus.getAddresses().get(0));
        assertIp4AddressEquals(ip4AddressStatus.getDnsServerAddresses().get(0));
        assertTrue(ip4AddressStatus.getGateway().isPresent());
        assertEquals(IPAddress.parseHostAddress("172.16.2.1"), ip4AddressStatus.getGateway().get());
    }

    private void assertInterfaceIp4AddressEquals(NetworkInterfaceIpAddress<IP4Address> address)
            throws UnknownHostException {
        assertEquals(IPAddress.parseHostAddress("172.16.2.100"), address.getAddress());
        assertEquals(16, address.getPrefix());
    }

    private void assertIp4AddressEquals(IP4Address address) throws UnknownHostException {
        assertEquals(IPAddress.parseHostAddress("172.16.2.23"), address);
    }

    private void assertEqualsIp6AddressStatus(NetworkInterfaceIpAddressStatus<IP6Address> ip6AddressStatus)
            throws UnknownHostException {
        assertInterfaceIp6AddressEquals(ip6AddressStatus.getAddresses().get(0));
        assertIp6AddressEquals(ip6AddressStatus.getDnsServerAddresses().get(0));
        assertEquals(IPAddress.parseHostAddress("2345:425:2CA1:0000:0000:567:5673:23b6"),
                ip6AddressStatus.getGateway().get());
    }

    private void assertInterfaceIp6AddressEquals(NetworkInterfaceIpAddress<IP6Address> address)
            throws UnknownHostException {
        assertEquals(IPAddress.parseHostAddress("2345:425:2CA1:0000:0000:567:5673:23b5"), address.getAddress());
        assertEquals(64, address.getPrefix());
    }

    private void assertIp6AddressEquals(IP6Address address) throws UnknownHostException {
        assertEquals(IPAddress.parseHostAddress("2345:425:2CA1:0000:0000:567:5673:23b7"), address);
    }

    private void createTestObjects() {
        this.nmDbusConnector = mock(NMDbusConnector.class);
        this.commandExecutorService = mock(CommandExecutorService.class);
        this.statusService = new NMStatusServiceImpl(this.nmDbusConnector);
        this.statusService.setCommandExecutorService(this.commandExecutorService);
    }
}
