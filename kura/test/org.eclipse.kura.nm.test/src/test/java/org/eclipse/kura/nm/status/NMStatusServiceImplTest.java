package org.eclipse.kura.nm.status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

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
import org.eclipse.kura.net.status.wifi.WifiAccessPoint;
import org.eclipse.kura.net.status.wifi.WifiAccessPoint.WifiAccessPointBuilder;
import org.eclipse.kura.net.status.wifi.WifiCapability;
import org.eclipse.kura.net.status.wifi.WifiInterfaceStatus;
import org.eclipse.kura.net.status.wifi.WifiInterfaceStatus.WifiInterfaceStatusBuilder;
import org.eclipse.kura.net.status.wifi.WifiMode;
import org.eclipse.kura.net.status.wifi.WifiRadioMode;
import org.eclipse.kura.net.status.wifi.WifiSecurity;
import org.eclipse.kura.nm.NMDbusConnector;
import org.eclipse.kura.usb.UsbNetDevice;
import org.freedesktop.dbus.exceptions.DBusException;
import org.junit.Test;

public class NMStatusServiceImplTest {

    private NMStatusServiceImpl statusService;
    private NMDbusConnector nmDbusConnector;
    private NetworkService networkService;
    private Optional<NetworkInterfaceStatus> status;

    @Test
    public void shouldReturnEmptyIfInterfaceDoesNotExist() throws DBusException, UnknownHostException {
        givenNMStatusServiceImpl();
        whenInterfaceStatusIsRetrieved("non-existing-interface");
        thenInterfaceStatusIsEmpty();
    }

    @Test
    public void shouldReturnExistingEthernetInterfaceStatus() throws DBusException, UnknownHostException {
        givenNMStatusServiceImpl();
        whenInterfaceStatusIsRetrieved("abcd0");
        thenInterfaceStatusIsReturned();
    }

    @Test
    public void shouldReturnFullEthernetInterfaceStatus() throws DBusException, UnknownHostException {
        givenNMStatusServiceImpl();
        whenInterfaceStatusIsRetrieved("abcd0");
        thenInterfaceStatusIsReturned();
        thenEthernetInterfaceStatusIsRetrieved();
        thenRetrievedEthernetInterfaceStatusHasFullProperties();
    }

    @Test
    public void shouldReturnExistingWiFiInterfaceStatus() throws DBusException, UnknownHostException {
        givenNMStatusServiceImpl();
        whenInterfaceStatusIsRetrieved("wlan0");
        thenInterfaceStatusIsReturned();
    }

    @Test
    public void shouldReturnFullWifiInterfaceStatus() throws DBusException, UnknownHostException {
        givenNMStatusServiceImpl();
        whenInterfaceStatusIsRetrieved("wlan0");
        thenInterfaceStatusIsReturned();
        thenWifiInterfaceStatusIsRetrieved();
        thenRetrievedWifiInterfaceStatusHasFullProperties();
    }

    private void givenNMStatusServiceImpl() throws DBusException, UnknownHostException {
        this.networkService = mock(NetworkService.class);
        this.nmDbusConnector = mock(NMDbusConnector.class);
        when(this.nmDbusConnector.getInterfaceStatus("abcd0", this.networkService))
                .thenReturn(buildEthernetInterfaceStatus("abcd0"));
        when(this.nmDbusConnector.getInterfaceStatus("non-existing-interface", networkService))
                .thenThrow(DBusException.class);
        when(this.nmDbusConnector.getInterfaceStatus("wlan0", this.networkService))
                .thenReturn(buildWifiInterfaceStatus("wlan0"));
        this.statusService = new NMStatusServiceImpl(this.nmDbusConnector);
        this.statusService.setNetworkService(this.networkService);
    }

    private void whenInterfaceStatusIsRetrieved(String interfaceName) {
        this.status = this.statusService.getNetworkStatus(interfaceName);
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
        assertEquals(2, wifiStatus.getSupportedBitrates().size());
        assertEquals(Arrays.asList(54L, 1000L), wifiStatus.getSupportedBitrates());
        assertEquals(2, wifiStatus.getSupportedRadioModes().size());
        assertEquals(EnumSet.of(WifiRadioMode.RADIO_MODE_80211A, WifiRadioMode.RADIO_MODE_80211G),
                wifiStatus.getSupportedRadioModes());
        assertEquals(4, wifiStatus.getSupportedChannels().size());
        assertEquals(Arrays.asList(1, 2, 3, 4), wifiStatus.getSupportedChannels());
        assertEquals(2, wifiStatus.getSupportedFrequencies().size());
        assertEquals(Arrays.asList(900L, 2400L), wifiStatus.getSupportedFrequencies());
        assertEquals("IT", wifiStatus.getCountryCode());
        assertEquals(WifiMode.INFRA, wifiStatus.getMode());
        assertTrue(wifiStatus.getActiveWifiAccessPoint().isPresent());
        assertEquals(buildAP(), wifiStatus.getActiveWifiAccessPoint().get());
        assertEquals(1, wifiStatus.getAvailableWifiAccessPoints().size());
        assertEquals(buildAP(), wifiStatus.getAvailableWifiAccessPoints().get(0));
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
        assertTrue(networkStatus.getInterfaceIp6Addresses().isPresent());
        assertEquals(buildIp6Address(), networkStatus.getInterfaceIp6Addresses().get());
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
        builder.withSupportedBitrates(Arrays.asList(54L, 1000L));
        builder.withSupportedRadioModes(EnumSet.of(WifiRadioMode.RADIO_MODE_80211A, WifiRadioMode.RADIO_MODE_80211G));
        builder.withSupportedChannels(Arrays.asList(1, 2, 3, 4));
        builder.withSupportedFrequencies(Arrays.asList(900L, 2400L));
        builder.withCountryCode("IT");
        builder.withMode(WifiMode.INFRA);
        builder.withActiveWifiAccessPoint(Optional.of(buildAP()));
        builder.withAvailableWifiAccessPoints(Arrays.asList(buildAP()));
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
        NetworkInterfaceIpAddressStatus<IP6Address> ip6AddressStatus = new NetworkInterfaceIpAddressStatus<>(
                ip6Address);
        ip6AddressStatus.setGateway((IP6Address) IPAddress.parseHostAddress("2345:425:2CA1:0000:0000:567:5673:23b6"));
        ip6AddressStatus
                .addDnsServerAddress((IP6Address) IPAddress.parseHostAddress("2345:425:2CA1:0000:0000:567:5673:23b7"));

        return ip6AddressStatus;
    }

    private WifiAccessPoint buildAP() {
        WifiAccessPointBuilder builder = WifiAccessPoint.builder();
        builder.withSsid("MyCoolAP");
        builder.withHardwareAddress(new byte[] { 0x00, 0x11, 0x02, 0x33, 0x44, 0x55 });
        builder.withFrequency(5000L);
        builder.withChannel(7);
        builder.withMode(WifiMode.INFRA);
        builder.withMaxBitrate(54);
        builder.withSignalQuality(78);
        builder.withWpaSecurity(EnumSet.of(WifiSecurity.GROUP_CCMP, WifiSecurity.GROUP_TKIP));
        builder.withRsnSecurity(EnumSet.of(WifiSecurity.GROUP_WEP104, WifiSecurity.KEY_MGMT_802_1X));
        return builder.build();
    }
}
