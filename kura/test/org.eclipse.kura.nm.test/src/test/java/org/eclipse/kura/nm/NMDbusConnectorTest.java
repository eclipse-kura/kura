package org.eclipse.kura.nm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.eclipse.kura.net.NetInterface;
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.eclipse.kura.net.NetworkService;
import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.NetworkInterfaceType;
import org.eclipse.kura.usb.UsbNetDevice;
import org.freedesktop.NetworkManager;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.networkmanager.Device;
import org.freedesktop.networkmanager.GetAppliedConnectionTuple;
import org.freedesktop.networkmanager.Settings;
import org.freedesktop.networkmanager.device.Generic;
import org.freedesktop.networkmanager.settings.Connection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class NMDbusConnectorTest {

    DBusConnection dbusConnection = mock(DBusConnection.class, RETURNS_SMART_NULLS);
    NetworkManager mockedNetworkManager = mock(NetworkManager.class);
    NMDbusConnector instanceNMDbusConnector;
    DBusConnection dbusConnectionInternal;

    Boolean hasDBusExceptionBeenThrown = false;
    Boolean hasNoSuchElementExceptionThrown = false;
    Boolean hasNullPointerExceptionThrown = false;
    
    NetworkInterfaceStatus netInterface;
    NetworkService networkService;
    
    Map<String, Device> mockDevices = new HashMap<>();

    List<String> internalStringList;
    Map<String, Object> netConfig = new HashMap<>();

    @Before
    public void setUpPersistantMocks() throws DBusException {
        givenBasicMockedDbusConnector();
    }
    
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
    public void getDbusConnectionShouldWork() throws DBusException {
        whenGetDbusConnectionIsRun();

        thenNoExceptionIsThrown();

        thenGetDbusConnectionIsMockedConnection();
    }

    @Test
    public void checkPermissionsShouldWork() throws DBusException {
        givenMockedPermissions();

        whenCheckPermissionsIsRun();

        thenNoExceptionIsThrown();
        thenCheckPermissionsRan();
    }

    @Test
    public void checkVersionShouldWork() throws DBusException {
        givenMockedVersion();

        whenCheckVersionIsRun();

        thenNoExceptionIsThrown();
        thenCheckVersionIsRun();
    }

    @Test
    public void getInterfacesShouldWork() throws DBusException {
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED);
        givenMockedDevice("wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED);
        givenMockedDeviceList();
        
        whenGetInterfaces();

        thenNoExceptionIsThrown();
        thenReturnedDevicesAre(Arrays.asList("wlan0", "eth0"));
    }

    @Test
    public void applyShouldDoNothingWithNoCache() throws DBusException {
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED);
        givenMockedDevice("wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED);
        givenMockedDeviceList();
        
        whenApply();

        thenNoExceptionIsThrown();
        thenNetoworkSettingsDoNotChangeForDevice("eth0");
        thenNetoworkSettingsDoNotChangeForDevice("wlan0");
    }

    @Test
    public void applyShouldThrowWithNullMap() throws DBusException {
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED);
        givenMockedDevice("wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED);
        givenMockedDeviceList();
        
        whenApplyIsCalledWith(null);

        thenNullPointerExceptionIsThrown();
    }

    @Test
    public void applyShouldThrowWithEmptyMap() throws DBusException {
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED);
        givenMockedDevice("wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED);
        givenMockedDeviceList();
        
        whenApplyIsCalledWith(new HashMap<String, Object>());

        thenNoSuchElementExceptionIsThrown();
    }

    @Test
    public void applyShouldDoNothingWithEnabledUnsupportedDevices() throws DBusException {
        givenMockedDevice("unused0", NMDeviceType.NM_DEVICE_TYPE_UNUSED1, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED);
        givenMockedDeviceList();

        givenNetworkConfigMapWith("net.interfaces", "unused0, ");
        givenNetworkConfigMapWith("net.interface.unused0.config.dhcpClient4.enabled", true);
        givenNetworkConfigMapWith("net.interface.unused0.config.ip4.status", "netIPv4StatusEnabledWAN");

        whenApplyIsCalledWith(netConfig);

        thenNoExceptionIsThrown();
        thenNetoworkSettingsDoNotChangeForDevice("unused0");
    }

    @Test
    public void applyShouldWorkWithEnabledEthernet() throws DBusException {
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_DISCONNECTED);
        givenMockedDeviceList();
        
        givenNetworkConfigMapWith("net.interfaces", "eth0, ");
        givenNetworkConfigMapWith("net.interface.eth0.config.dhcpClient4.enabled", false);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.address", "192.168.0.12");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.prefix", (short) 25);
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");

        whenApplyIsCalledWith(this.netConfig);

        thenNoExceptionIsThrown();
        thenConnectDisconnectIsCalledFor("eth0");
    }

    @Test
    public void applyShouldWorkWithDisabledEthernet() throws DBusException {
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_ACTIVATED);
        givenMockedDeviceList();
        
        givenNetworkConfigMapWith("net.interfaces", "eth,");
        givenNetworkConfigMapWith("net.interface.eth0.config.ip4.status", "netIPv4StatusDisabled");

        whenApplyIsCalledWith(this.netConfig);

        thenNoExceptionIsThrown();
        thenDisconnectIsCalledFor("eth0");
    }

    @Test
    public void applyShouldNotDisableLoopbackDevice() throws DBusException {
        givenMockedDevice("lo", NMDeviceType.NM_DEVICE_TYPE_LOOPBACK, NMDeviceState.NM_DEVICE_STATE_ACTIVATED);
        givenMockedDeviceList();
        
        givenNetworkConfigMapWith("net.interfaces", "lo,");
        givenNetworkConfigMapWith("net.interface.lo.config.ip4.status", "netIPv4StatusDisabled");

        whenApplyIsCalledWith(this.netConfig);

        thenNoExceptionIsThrown();
        thenNetoworkSettingsDoNotChangeForDevice("lo");
    }
    
    @Test
    public void applyShouldNotDisableLoopbackDeviceOldVersionOfNM() throws DBusException {
        givenMockedDevice("lo", NMDeviceType.NM_DEVICE_TYPE_GENERIC, NMDeviceState.NM_DEVICE_STATE_ACTIVATED);
        givenMockedDeviceList();
        
        givenNetworkConfigMapWith("net.interfaces", "lo,");
        givenNetworkConfigMapWith("net.interface.lo.config.ip4.status", "netIPv4StatusDisabled");
        
        whenApplyIsCalledWith(this.netConfig);
        
        thenNoExceptionIsThrown();
        thenNetoworkSettingsDoNotChangeForDevice("lo");
    }
    
    @Test
    public void getInterfaceStatusShouldWorkEthernet() throws DBusException {
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_ACTIVATED);
        givenExtraStatusMocksFor("eth0", NMDeviceState.NM_DEVICE_STATE_ACTIVATED);
        givenMockedDeviceList();
        givenNetworkServiceThatAlwaysReturnsEmpty();
        

        whenGetInterfaceStatus("eth0", networkService);

        thenNoExceptionIsThrown();
        thenInterfaceStatusIsNotNull();
        thenNetInterfaceTypeIs(NetworkInterfaceType.ETHERNET);
    }
    
    @Test
    public void getInterfaceStatusShouldWorkLoopback() throws DBusException {
        givenMockedDevice("lo", NMDeviceType.NM_DEVICE_TYPE_LOOPBACK, NMDeviceState.NM_DEVICE_STATE_FAILED);
        givenExtraStatusMocksFor("lo", NMDeviceState.NM_DEVICE_STATE_FAILED);
        givenMockedDeviceList();
        givenNetworkServiceThatAlwaysReturnsEmpty();
        
        whenGetInterfaceStatus("lo", networkService);
        
        thenNoExceptionIsThrown();
        thenInterfaceStatusIsNotNull();
        thenNetInterfaceTypeIs(NetworkInterfaceType.LOOPBACK);
    }
    
    @Test
    public void getInterfaceStatusShouldWorkUnsuported() throws DBusException {
        givenMockedDevice("unused0", NMDeviceType.NM_DEVICE_TYPE_UNUSED1, NMDeviceState.NM_DEVICE_STATE_FAILED);
        givenExtraStatusMocksFor("unused0", NMDeviceState.NM_DEVICE_STATE_FAILED);
        givenMockedDeviceList();
        givenNetworkServiceThatAlwaysReturnsEmpty();
        
        whenGetInterfaceStatus("unused0", networkService);
        
        thenNoExceptionIsThrown();
        thenInterfaceStatusIsNull();
    }
    
    @Test
    public void getInterfaceStatusShouldWorkWireless() throws DBusException {
        givenMockedDevice("wlan0", NMDeviceType.NM_DEVICE_TYPE_WIFI, NMDeviceState.NM_DEVICE_STATE_FAILED);
        givenExtraStatusMocksFor("wlan0", NMDeviceState.NM_DEVICE_STATE_FAILED);
        givenMockedDeviceList();
        givenNetworkServiceThatAlwaysReturnsEmpty();
        
        whenGetInterfaceStatus("wlan0", networkService);
        
        thenNoExceptionIsThrown();
        thenInterfaceStatusIsNull();
    }
    
    @Test
    public void getInterfaceStatusShouldWorkEthernetUSB() throws DBusException {
        givenMockedDevice("eth0", NMDeviceType.NM_DEVICE_TYPE_ETHERNET, NMDeviceState.NM_DEVICE_STATE_ACTIVATED);
        givenExtraStatusMocksFor("eth0", NMDeviceState.NM_DEVICE_STATE_ACTIVATED);
        givenMockedDeviceList();
        givenNetworkServiceMockedForUsbInterfaceWithName("eth0");
        

        whenGetInterfaceStatus("eth0", networkService);

        thenNoExceptionIsThrown();
        thenInterfaceStatusIsNotNull();
        thenNetInterfaceTypeIs(NetworkInterfaceType.ETHERNET);
    }

    public void givenBasicMockedDbusConnector() throws DBusException {
		when(dbusConnection.getRemoteObject(eq("org.freedesktop.NetworkManager"), eq("/org/freedesktop/NetworkManager"), any()))
					.thenReturn(mockedNetworkManager);
		
		this.instanceNMDbusConnector = NMDbusConnector.getInstance(this.dbusConnection);
	}

    public void givenMockedPermissions() {

        Map<String, String> tempPerms = new HashMap<>();

        tempPerms.put("test1", "testVal1");
        tempPerms.put("test2", "testVal2");
        tempPerms.put("test3", "testVal3");

        when(mockedNetworkManager.GetPermissions()).thenReturn(tempPerms);

    }

    public void givenMockedVersion() throws DBusException {

        Properties mockProps = mock(org.freedesktop.dbus.interfaces.Properties.class);
        when(mockProps.Get(eq("org.freedesktop.NetworkManager"), eq("Version"))).thenReturn("Mock-Version");

        doReturn(mockProps).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/org/freedesktop/NetworkManager"), eq(Properties.class));
    }
    
    public void givenMockedDevice(String interfaceName, NMDeviceType type, NMDeviceState state) throws DBusException {
        Device mockedDevice1 = mock(Device.class);
        
        this.mockDevices.put(interfaceName, mockedDevice1);
            
        when(mockedDevice1.getObjectPath()).thenReturn("/mock/device/" + interfaceName);

        Generic mockedDevice1Generic = mock(Generic.class);
        when(mockedDevice1Generic.getObjectPath()).thenReturn("/mock/device/lo");
        
        DBusPath mockedPath1 = mock(DBusPath.class);
        when(mockedPath1.getPath()).thenReturn("/mock/device/" + interfaceName);

        Map<String, Map<String, Variant<?>>> mockedDevice1ConnectionSetting = new HashMap<String, Map<String, Variant<?>>>();
        mockedDevice1ConnectionSetting.put("connection",
                Collections.singletonMap("uuid", new Variant<>("mock-uuid-123")));

        Settings mockedDevice1Settings = mock(Settings.class);
        when(mockedDevice1Settings.GetConnectionByUuid(eq("mock-uuid-123"))).thenReturn(mockedPath1);

        Connection mockedDevice1Connection = mock(Connection.class, RETURNS_SMART_NULLS);
        when(mockedDevice1Connection.GetSettings()).thenReturn(mockedDevice1ConnectionSetting);

        GetAppliedConnectionTuple mockedDevice1ConnectionTouple = mock(GetAppliedConnectionTuple.class);
        when(mockedDevice1ConnectionTouple.getConnection()).thenReturn(mockedDevice1ConnectionSetting);

        when(mockedDevice1.GetAppliedConnection(eq(new UInt32(0)))).thenReturn(mockedDevice1ConnectionTouple);

        Properties mockedProperties1 = mock(Properties.class);
        
        if(interfaceName.equals("lo")) {
            when(mockedProperties1.Get(any(), any())).thenReturn("loopback");            
        }
        
        when(mockedProperties1.Get(eq("org.freedesktop.NetworkManager.Device"), eq("DeviceType")))
                .thenReturn(NMDeviceType.toUInt32(type));
        when(mockedProperties1.Get(eq("org.freedesktop.NetworkManager.Device"), eq("State")))
                .thenReturn(NMDeviceState.toUInt32(state));
        when(mockedProperties1.Get(eq("org.freedesktop.NetworkManager.Device"), eq("Interface"))).thenReturn(interfaceName);


        when(this.mockedNetworkManager.GetDeviceByIpIface(eq(interfaceName))).thenReturn(mockedPath1);

        doReturn(mockedDevice1).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/mock/device/" + interfaceName), eq(Device.class));
        doReturn(mockedDevice1Settings).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/org/freedesktop/NetworkManager/Settings"), eq(Settings.class));
        doReturn(mockedProperties1).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/mock/device/" + interfaceName), eq(Properties.class));
        doReturn(mockedDevice1Connection).when(this.dbusConnection)
                .getRemoteObject(eq("org.freedesktop.NetworkManager"), eq("/mock/device/" + interfaceName), eq(Connection.class));
        doReturn(mockedDevice1Generic).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/mock/device/lo"), eq(Generic.class));
    }
    
    public void givenExtraStatusMocksFor(String interfaceName, NMDeviceState state) throws DBusException {
        Device mockedDevice1 = this.mockDevices.get(interfaceName);
        Properties mockedProperties = this.dbusConnection.getRemoteObject("org.freedesktop.NetworkManager",
                "/mock/device/" + interfaceName, Properties.class);
        
        when(mockedProperties.Get(eq("org.freedesktop.NetworkManager.Device"), eq("Autoconnect"))).thenReturn(true);
        when(mockedProperties.Get(eq("org.freedesktop.NetworkManager.Device"), eq("FirmwareVersion"))).thenReturn("firmware");
        when(mockedProperties.Get(eq("org.freedesktop.NetworkManager.Device"), eq("Driver"))).thenReturn("driver");
        when(mockedProperties.Get(eq("org.freedesktop.NetworkManager.Device"), eq("DriverVersion"))).thenReturn("1.0.0");
        when(mockedProperties.Get(eq("org.freedesktop.NetworkManager.Device"), eq("State"))).thenReturn(NMDeviceState.toUInt32(state));
        when(mockedProperties.Get(eq("org.freedesktop.NetworkManager.Device"), eq("Mtu"))).thenReturn(new UInt32(100));
        when(mockedProperties.Get(eq("org.freedesktop.NetworkManager.Device"), eq("HwAddress"))).thenReturn("F5:5B:32:7C:40:EA");
        
        DBusPath path = mock(DBusPath.class);
        when(path.getPath()).thenReturn("/");
        
        when(mockedProperties.Get(eq("org.freedesktop.NetworkManager.Device"), eq("Ip4Config"))).thenReturn(path);

    }
    
    public void givenMockedDeviceList() {
        
        List<DBusPath> devicePaths = new ArrayList<>();
        
        for(String interfaceName : this.mockDevices.keySet()) {
            devicePaths.add(this.mockedNetworkManager.GetDeviceByIpIface(interfaceName));
        }
        when(this.mockedNetworkManager.GetAllDevices()).thenReturn(devicePaths);
    }
    
    public void givenNetworkConfigMapWith(String key, Object value) {
        netConfig.put(key, value);
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
            hasDBusExceptionBeenThrown = true;
        }
    }

    public void whenGetInterfaces() {
        try {
            this.internalStringList = this.instanceNMDbusConnector.getInterfaces();
        } catch (DBusException e) {
            hasDBusExceptionBeenThrown = true;
        }
    }

    public void whenApply() {
        try {
            this.instanceNMDbusConnector.apply();
        } catch (DBusException e) {
            hasDBusExceptionBeenThrown = true;
        }
    }

    public void whenApplyIsCalledWith(Map<String, Object> networkConfig) {
        try {
            this.instanceNMDbusConnector.apply(networkConfig);
        } catch (DBusException e) {
            e.printStackTrace();
            hasDBusExceptionBeenThrown = true;
        } catch (NoSuchElementException e) {
            e.printStackTrace();
            hasNoSuchElementExceptionThrown = true;
        } catch (NullPointerException e) {
            e.printStackTrace();
            hasNullPointerExceptionThrown = true;
        }
    }
    
    public void whenGetInterfaceStatus(String netInterface, NetworkService netService) {
        try {
            this.netInterface = this.instanceNMDbusConnector.getInterfaceStatus(netInterface, netService);
        } catch (DBusException e) {
            e.printStackTrace();
            hasDBusExceptionBeenThrown = true;
        } catch (NoSuchElementException e) {
            e.printStackTrace();
            hasNoSuchElementExceptionThrown = true;
        } catch (NullPointerException e) {
            e.printStackTrace();
            hasNullPointerExceptionThrown = true;
        }
    }

    public void thenNoExceptionIsThrown() {
        assertFalse(hasDBusExceptionBeenThrown);
        assertFalse(hasNoSuchElementExceptionThrown);
    }

    public void thenDBusExceptionIsThrown() {
        assertTrue(hasDBusExceptionBeenThrown);
    }

    public void thenNullPointerExceptionIsThrown() {
        assertTrue(hasNullPointerExceptionThrown);
    }

    public void thenNoSuchElementExceptionIsThrown() {
        assertTrue(hasNoSuchElementExceptionThrown);
    }

    public void thenGetDbusConnectionIsMockedConnection() {
        assertEquals(this.dbusConnection, this.dbusConnectionInternal);
    }

    public void thenConnectionClosed() {
        verify(this.dbusConnection, atLeastOnce()).disconnect();
    }

    public void thenCheckVersionIsRun() throws DBusException {
        verify(this.dbusConnection, atLeastOnce()).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/org/freedesktop/NetworkManager"), any());
    }

    public void thenCheckPermissionsRan() {
        verify(this.mockedNetworkManager, atLeastOnce()).GetPermissions();
    }

    public void thenReturnedDevicesAre(List<String> list) {
        assertEquals(list, this.internalStringList);
    }
    
    public void thenDisconnectIsCalledFor(String netInterface) {
        verify(this.mockDevices.get(netInterface)).Disconnect();
    }
    
    public void thenConnectDisconnectIsCalledFor(String netInterface) throws DBusException {
        Connection connect = this.dbusConnection.getRemoteObject("org.freedesktop.NetworkManager", "/mock/device/" + netInterface, Connection.class);
        verify(connect).Update(any());
    }
    
    public void thenNetoworkSettingsDoNotChangeForDevice(String netInterface) throws DBusException {
        
        Connection connect = this.dbusConnection.getRemoteObject("org.freedesktop.NetworkManager", "/mock/device/" + netInterface, Connection.class);
        verify(connect, never()).Update(any());
        verify(this.mockDevices.get(netInterface), never()).Disconnect();
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
 
}
