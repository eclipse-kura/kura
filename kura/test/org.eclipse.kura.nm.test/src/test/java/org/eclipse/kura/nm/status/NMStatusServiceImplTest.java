package org.eclipse.kura.nm.status;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetworkService;
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
import org.eclipse.kura.net.status.wifi.WifiAccessPoint;
import org.eclipse.kura.net.status.wifi.WifiAccessPoint.WifiAccessPointBuilder;
import org.eclipse.kura.net.status.wifi.WifiCapability;
import org.eclipse.kura.net.status.wifi.WifiChannel;
import org.eclipse.kura.net.status.wifi.WifiInterfaceStatus;
import org.eclipse.kura.net.status.wifi.WifiInterfaceStatus.WifiInterfaceStatusBuilder;
import org.eclipse.kura.net.status.wifi.WifiMode;
import org.eclipse.kura.net.status.wifi.WifiSecurity;
import org.eclipse.kura.nm.NMDbusConnector;
import org.eclipse.kura.usb.UsbNetDevice;
import org.freedesktop.dbus.errors.UnknownMethod;
import org.freedesktop.dbus.exceptions.DBusException;
import org.junit.Test;

public class NMStatusServiceImplTest {

    private NMStatusServiceImpl statusService;
    private NMDbusConnector nmDbusConnector;
    private NetworkService networkService;
    private CommandExecutorService commandExecutorService;
    private Optional<NetworkInterfaceStatus> status;
    private List<NetworkInterfaceStatus> statuses;
    private List<String> interfaceNames;

    @Test
    public void shouldReturnEmptyIfInterfaceDoesNotExist() throws DBusException, UnknownHostException, KuraException {
        givenNMStatusServiceImplWithNonExistingInterface();
        whenInterfaceStatusIsRetrieved("non-existing-interface");
        thenInterfaceStatusIsEmpty();
    }

    @Test
    public void shouldReturnEmptyIfDBusObjectVanishes() throws DBusException, UnknownHostException, KuraException {
        givenNMStatusServiceImplThrowingUnknownMethod();
        whenInterfaceStatusIsRetrieved("wlan1");
        thenInterfaceStatusIsEmpty();
    }

    @Test
    public void shouldReturnExistingEthernetInterfaceStatus()
            throws DBusException, UnknownHostException, KuraException {
        givenNMStatusServiceImplWithEthernetInterface();
        whenInterfaceStatusIsRetrieved("abcd0");
        thenInterfaceStatusIsReturned();
    }

    @Test
    public void shouldReturnFullEthernetInterfaceStatus() throws DBusException, UnknownHostException, KuraException {
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
    public void shouldReturnEmptyListIfInterfacesDoNotExist() throws DBusException, UnknownHostException {
        givenNMStatusServiceImplWithoutInterfaces();
        whenInterfaceStatusesAreRetrieved();
        thenInterfaceStatusListIsEmpty();
    }

    @Test
    public void shouldReturnExistingInterfaceStatuses() throws DBusException, UnknownHostException, KuraException {
        givenNMStatusServiceImplWithInterfaces();
        whenInterfaceStatusesAreRetrieved();
        thenInterfaceStatusListIsReturned();
    }

    @Test
    public void shouldReturnEmptyInterfaceNameList() throws UnknownHostException, DBusException {
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

    private void givenNMStatusServiceImplWithNonExistingInterface()
            throws DBusException, UnknownHostException, KuraException {
        createTestObjects();
        when(this.nmDbusConnector.getInterfaceStatus("non-existing-interface", networkService,
                this.commandExecutorService)).thenThrow(DBusException.class);
    }

    private void givenNMStatusServiceImplThrowingUnknownMethod() throws DBusException, KuraException {
        createTestObjects();
        when(this.nmDbusConnector.getInterfaceStatus("wlan0", networkService, this.commandExecutorService))
                .thenThrow(UnknownMethod.class);
    }

    private void givenNMStatusServiceImplWithEthernetInterface()
            throws DBusException, UnknownHostException, KuraException {
        createTestObjects();
        when(this.nmDbusConnector.getInterfaceStatus("abcd0", this.networkService, this.commandExecutorService))
                .thenReturn(buildEthernetInterfaceStatus("abcd0"));
    }

    private void givenNMStatusServiceImplWithWifiInterface() throws DBusException, UnknownHostException, KuraException {
        createTestObjects();
        when(this.nmDbusConnector.getInterfaceStatus("wlan0", this.networkService, this.commandExecutorService))
                .thenReturn(buildWifiInterfaceStatus("wlan0"));
    }

    private void givenNMStatusServiceImplWithoutInterfaces() throws DBusException, UnknownHostException {
        createTestObjects();
        when(this.nmDbusConnector.getInterfaces()).thenThrow(DBusException.class);
    }

    private void givenNMStatusServiceImplWithInterfaces() throws DBusException, UnknownHostException, KuraException {
        createTestObjects();
        when(this.nmDbusConnector.getInterfaces()).thenReturn(Arrays.asList("abcd0", "wlan0"));
        when(this.nmDbusConnector.getInterfaceStatus("abcd0", this.networkService, this.commandExecutorService))
                .thenReturn(buildEthernetInterfaceStatus("abcd0"));
        when(this.nmDbusConnector.getInterfaceStatus("wlan0", this.networkService, this.commandExecutorService))
                .thenReturn(buildWifiInterfaceStatus("wlan0"));
    }

    private void givenNMStatusServiceImplWithLoopbackInterface()
            throws UnknownHostException, DBusException, KuraException {
        createTestObjects();
        when(this.nmDbusConnector.getInterfaceStatus("lo", this.networkService, this.commandExecutorService))
                .thenReturn(buildLoopbackInterfaceStatus("lo"));
    }

    private void whenInterfaceStatusIsRetrieved(String interfaceName) {
        this.status = this.statusService.getNetworkStatus(interfaceName);
    }

    private void whenInterfaceStatusesAreRetrieved() {
        this.statuses = this.statusService.getNetworkStatus();
    }

    private void whenInterfaceNameListIsRetrived() {
        this.interfaceNames = this.statusService.getInterfaceNames();
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
        assertEquals("abcd0", ethStatus.getName());
        assertCommonProperties(ethStatus);
        assertEquals(new UsbNetDevice("1234", "5678", "CoolManufacturer", "VeryCoolModem", "1", "3", "abcd0"),
                ethStatus.getUsbNetDevice().get());
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
        assertEquals("wlan0", wifiStatus.getName());
        assertCommonProperties(wifiStatus);
        assertEquals(new UsbNetDevice("1234", "5678", "CoolManufacturer", "VeryCoolModem", "1", "3", "wlan0"),
                wifiStatus.getUsbNetDevice().get());
        assertEquals(2, wifiStatus.getCapabilities().size());
        assertEquals(EnumSet.of(WifiCapability.AP, WifiCapability.FREQ_2GHZ), wifiStatus.getCapabilities());
        assertEquals(4, wifiStatus.getChannels().size());
        assertEquals(Arrays.asList(new WifiChannel(1, 2412), new WifiChannel(2, 2417), new WifiChannel(3, 2422),
                new WifiChannel(4, 2432)), wifiStatus.getChannels());
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

    private void thenInterfaceStatusListIsEmpty() {
        assertNotNull(this.statuses);
        assertTrue(this.statuses.isEmpty());
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

    private void thenInterfaceStatusListIsReturned() {
        assertNotNull(this.statuses);
        assertFalse(this.statuses.isEmpty());
        assertEquals(2, this.statuses.size());
        this.statuses.forEach(status -> {
            assertTrue(status.getName().equals("wlan0") || status.getName().equals("abcd0"));
        });
    }

    private void thenLoopbackInterfaceStatusIsRetrieved() {
        assertTrue(this.status.get() instanceof LoopbackInterfaceStatus);
        assertEquals(NetworkInterfaceType.LOOPBACK, this.status.get().getType());
    }

    private void thenRetrievedLoopbackInterfaceStatusHasFullProperties() throws UnknownHostException {
        LoopbackInterfaceStatus loStatus = (LoopbackInterfaceStatus) this.status.get();
        assertEquals("lo", loStatus.getName());
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
        assertFalse(loStatus.getUsbNetDevice().isPresent());
        assertEquals(buildLoopbackInterfaceStatus("lo"), loStatus);
        assertEquals(buildLoopbackInterfaceStatus("lo").hashCode(), loStatus.hashCode());
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
        assertTrue(networkStatus.getUsbNetDevice().isPresent());
    }

    private EthernetInterfaceStatus buildEthernetInterfaceStatus(String interfaceName) throws UnknownHostException {
        EthernetInterfaceStatusBuilder builder = EthernetInterfaceStatus.builder();
        buildCommonProperties(interfaceName, builder);
        builder.withUsbNetDevice(
                Optional.of(new UsbNetDevice("1234", "5678", "CoolManufacturer", "VeryCoolModem", "1", "3", "abcd0")));
        builder.withIsLinkUp(true);
        return builder.build();
    }

    private WifiInterfaceStatus buildWifiInterfaceStatus(String interfaceName) throws UnknownHostException {
        WifiInterfaceStatusBuilder builder = WifiInterfaceStatus.builder();
        buildCommonProperties(interfaceName, builder);
        builder.withUsbNetDevice(
                Optional.of(new UsbNetDevice("1234", "5678", "CoolManufacturer", "VeryCoolModem", "1", "3", "wlan0")));
        builder.withCapabilities(EnumSet.of(WifiCapability.AP, WifiCapability.FREQ_2GHZ));
        builder.withWifiChannels(Arrays.asList(new WifiChannel(1, 2412), new WifiChannel(2, 2417),
                new WifiChannel(3, 2422), new WifiChannel(4, 2432)));
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

    private void buildCommonProperties(String interfaceName, NetworkInterfaceStatusBuilder<?> builder)
            throws UnknownHostException {
        builder.withName(interfaceName);
        builder.withHardwareAddress(new byte[] { 0x00, 0x11, 0x02, 0x33, 0x44, 0x55 });
        builder.withDriver("EthDriver").withDriverVersion("EthDriverVersion").withFirmwareVersion("1234");
        builder.withVirtual(false).withState(NetworkInterfaceState.ACTIVATED).withAutoConnect(true).withMtu(1500);
        builder.withInterfaceIp4Addresses(Optional.of(buildIp4Address()));
        builder.withInterfaceIp6Addresses(Optional.of(buildIp6Address()));
    }

    private NetworkInterfaceIpAddressStatus<IP4Address> buildIp4Address() throws UnknownHostException {
        NetworkInterfaceIpAddress<IP4Address> ip4Address = new NetworkInterfaceIpAddress<IP4Address>(
                (IP4Address) IPAddress.parseHostAddress("172.16.2.100"), (short) 16);
        NetworkInterfaceIpAddressStatus<IP4Address> ip4AddressStatus = new NetworkInterfaceIpAddressStatus<>(
                ip4Address);
        ip4AddressStatus.setGateway((IP4Address) IPAddress.parseHostAddress("172.16.2.1"));
        ip4AddressStatus.addDnsServerAddress((IP4Address) IPAddress.parseHostAddress("172.16.2.23"));

        return ip4AddressStatus;
    }

    private NetworkInterfaceIpAddressStatus<IP6Address> buildIp6Address() throws UnknownHostException {
        NetworkInterfaceIpAddress<IP6Address> ip6Address = new NetworkInterfaceIpAddress<IP6Address>(
                (IP6Address) IPAddress.parseHostAddress("2345:425:2CA1:0000:0000:567:5673:23b5"), (short) 64);
        NetworkInterfaceIpAddressStatus<IP6Address> ip6AddressStatus = new NetworkInterfaceIpAddressStatus<>();
        ip6AddressStatus.addAddress(ip6Address);
        ip6AddressStatus.setGateway((IP6Address) IPAddress.parseHostAddress("2345:425:2CA1:0000:0000:567:5673:23b6"));
        ip6AddressStatus
                .addDnsServerAddress((IP6Address) IPAddress.parseHostAddress("2345:425:2CA1:0000:0000:567:5673:23b7"));

        return ip6AddressStatus;
    }

    private WifiAccessPoint buildAP() {
        WifiAccessPointBuilder builder = WifiAccessPoint.builder();
        builder.withSsid("MyCoolAP");
        builder.withHardwareAddress(new byte[] { 0x00, 0x11, 0x02, 0x33, 0x44, 0x55 });
        builder.withChannel(new WifiChannel(7, 2442));
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
        assertEquals(new WifiChannel(7, 2442), ap.getChannel());
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
        assertEquals((IP4Address) IPAddress.parseHostAddress("172.16.2.1"), ip4AddressStatus.getGateway().get());
    }

    private void assertInterfaceIp4AddressEquals(NetworkInterfaceIpAddress<IP4Address> address)
            throws UnknownHostException {
        assertEquals(IP4Address.parseHostAddress("172.16.2.100"), address.getAddress());
        assertEquals(16, address.getPrefix());
    }

    private void assertIp4AddressEquals(IP4Address address) throws UnknownHostException {
        assertEquals(IPAddress.parseHostAddress("172.16.2.23"), address);
    }

    private void assertEqualsIp6AddressStatus(NetworkInterfaceIpAddressStatus<IP6Address> ip6AddressStatus)
            throws UnknownHostException {
        assertInterfaceIp6AddressEquals(ip6AddressStatus.getAddresses().get(0));
        assertIp6AddressEquals(ip6AddressStatus.getDnsServerAddresses().get(0));
        assertEquals((IP6Address) IPAddress.parseHostAddress("2345:425:2CA1:0000:0000:567:5673:23b6"),
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
        this.networkService = mock(NetworkService.class);
        this.nmDbusConnector = mock(NMDbusConnector.class);
        this.commandExecutorService = mock(CommandExecutorService.class);
        this.statusService = new NMStatusServiceImpl(this.nmDbusConnector);
        this.statusService.setNetworkService(this.networkService);
        this.statusService.setCommandExecutorService(this.commandExecutorService);
    }
}
