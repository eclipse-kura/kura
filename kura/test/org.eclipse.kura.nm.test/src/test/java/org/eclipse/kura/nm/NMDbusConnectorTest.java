package org.eclipse.kura.nm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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

public class NMDbusConnectorTest {

    DBusConnection dbusConnection = mock(DBusConnection.class, RETURNS_SMART_NULLS);
    NetworkManager mockedNetworkManager = mock(NetworkManager.class);
    NMDbusConnector instanceNMDbusConnector;
    DBusConnection dbusConnectionInternal;

    Boolean hasDBusExceptionBeenThrown = false;
    Boolean hasNoSuchElementExceptionThrown = false;
    Boolean hasNullPointerExceptionThrown = false;

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

        thenVerifyNoExceptionIsThrown();

        thenGetDbusConnectionIsMockedConnection();
    }

    @Test
    public void closeConnectionShouldWork() throws DBusException {
        whenCloseConnectionIsRun();

        thenVerifyNoExceptionIsThrown();

        thenVerifyConnectionClosed();
    }

    @Test
    public void checkPermissionsShouldWork() throws DBusException {
        givenMockedPermissions();

        whenCheckPermissionsIsRun();

        thenVerifyNoExceptionIsThrown();
        thenVerifyCheckPermissionsRan();
    }

    @Test
    public void checkVersionShouldWork() throws DBusException {
        givenMockedVersion();

        whenCheckVersionIsRun();

        thenVerifyNoExceptionIsThrown();
        thenVerifyCheckVersionIsRun();
    }

    @Test
    public void getInterfacesShouldWork() throws DBusException {
        givenMockedDevices();

        whenGetInterfaces();

        thenVerifyNoExceptionIsThrown();
        thenReturnedStringListIs(Arrays.asList("mockedDevice1", "mockedDevice2"));
    }

    @Test
    public void applyShouldDoNothingWithNoCache() throws DBusException {
        givenMockedDevices();

        whenApply();

        thenVerifyNoExceptionIsThrown();
        // check if nothing happened
    }

    @Test
    public void applyShouldThrowWithNullMap() throws DBusException {
        givenMockedDevices();

        whenApplyWithNetowrkConfig(null);

        thenNullPointerExceptionIsThrown();
    }

    @Test
    public void applyShouldThrowWithEmptyMap() throws DBusException {
        givenMockedDevices();

        whenApplyWithNetowrkConfig(new HashMap<String, Object>());

        thenVerifyNoSuchElementExceptionIsThrown();
    }

    @Test
    public void applyShouldWorkWithEnabledUnsupportedDevices() throws DBusException {

        givenMockedDeviceConfiguredToBeUnsupported();
        givenNetworkConfigWhichEnablesEth0();

        whenApplyWithNetowrkConfig(netConfig);

        thenVerifyNoExceptionIsThrown();
        // check if nothing happened
    }

    @Test
    public void applyShouldWorkWithEnabledEthernet() throws DBusException {
        givenFullyMockedEthernetDevice();
        givenNetworkConfigWhichEnablesEth0();

        whenApplyWithNetowrkConfig(this.netConfig);

        thenVerifyNoExceptionIsThrown();
        // TODO: Verify
    }

    @Test
    public void applyShouldWorkWithDisabledEthernet() throws DBusException {

        givenFullyMockedEthernetDevice();
        givenNetworkConfigWhichDisablesEth0();

        whenApplyWithNetowrkConfig(this.netConfig);

        thenVerifyNoExceptionIsThrown();
        // TODO: Verify
    }

    @Test
    public void applyShouldNotDisableLoopbackDevice() throws DBusException {

        givenFullyMockedLoopbackDevice();
        givenNetworkConfigWhichDisablesLoopbackDevice();

        whenApplyWithNetowrkConfig(this.netConfig);

        thenVerifyNoExceptionIsThrown();
        // TODO: Verify
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

    public void givenMockedDevices() throws DBusException {

        Device mockedDevice1 = mock(Device.class);
        when(mockedDevice1.getObjectPath()).thenReturn("/mock/device/path1");
        DBusPath mockedPath1 = mock(DBusPath.class);
        when(mockedPath1.getPath()).thenReturn("/mock/device/path1");
        Properties mockedProperties1 = mock(Properties.class);
        when(mockedProperties1.Get(eq("org.freedesktop.NetworkManager.Device"), eq("DeviceType")))
                .thenReturn(new UInt32(1));
        when(mockedProperties1.Get(eq("org.freedesktop.NetworkManager.Device"), eq("Interface")))
                .thenReturn("mockedDevice1");

        Device mockedDevice2 = mock(Device.class);
        when(mockedDevice2.getObjectPath()).thenReturn("/mock/device/path2");
        DBusPath mockedPath2 = mock(DBusPath.class);
        when(mockedPath2.getPath()).thenReturn("/mock/device/path2");
        Properties mockedProperties2 = mock(Properties.class);
        when(mockedProperties2.Get(eq("org.freedesktop.NetworkManager.Device"), eq("DeviceType")))
                .thenReturn(new UInt32(1));
        when(mockedProperties2.Get(eq("org.freedesktop.NetworkManager.Device"), eq("Interface")))
                .thenReturn("mockedDevice2");

        when(this.mockedNetworkManager.GetAllDevices()).thenReturn(Arrays.asList(mockedPath1, mockedPath2));

        doReturn(mockedDevice1).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/mock/device/path1"), eq(Device.class));
        doReturn(mockedProperties1).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/mock/device/path1"), eq(Properties.class));

        doReturn(mockedDevice2).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/mock/device/path2"), eq(Device.class));
        doReturn(mockedProperties2).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/mock/device/path2"), eq(Properties.class));
    }

    public void givenFullyMockedEthernetDevice() throws DBusException {
        Device mockedDevice1 = mock(Device.class);
        when(mockedDevice1.getObjectPath()).thenReturn("/mock/device/eth0");

        DBusPath mockedPath1 = mock(DBusPath.class);
        when(mockedPath1.getPath()).thenReturn("/mock/device/eth0");

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
        when(mockedProperties1.Get(eq("org.freedesktop.NetworkManager.Device"), eq("DeviceType")))
                .thenReturn(new UInt32(1)); // 1 id Ethernet
        when(mockedProperties1.Get(eq("org.freedesktop.NetworkManager.Device"), eq("State")))
                .thenReturn(new UInt32(10));
        when(mockedProperties1.Get(eq("org.freedesktop.NetworkManager.Device"), eq("Interface"))).thenReturn("eth0");

        when(this.mockedNetworkManager.GetAllDevices()).thenReturn(Arrays.asList(mockedPath1));

        when(this.mockedNetworkManager.GetDeviceByIpIface(eq("eth0"))).thenReturn(mockedPath1);

        doReturn(mockedDevice1).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/mock/device/eth0"), eq(Device.class));
        doReturn(mockedDevice1Settings).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/org/freedesktop/NetworkManager/Settings"), eq(Settings.class));
        doReturn(mockedProperties1).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/mock/device/eth0"), eq(Properties.class));
        doReturn(mockedDevice1Connection).when(this.dbusConnection)
                .getRemoteObject(eq("org.freedesktop.NetworkManager"), eq("/mock/device/eth0"), eq(Connection.class));
    }

    public void givenFullyMockedLoopbackDevice() throws DBusException {
        Device mockedDevice1 = mock(Device.class);
        when(mockedDevice1.getObjectPath()).thenReturn("/mock/device/lo");

        Generic mockedDevice1Generic = mock(Generic.class);
        when(mockedDevice1Generic.getObjectPath()).thenReturn("/mock/device/lo");

        DBusPath mockedPath1 = mock(DBusPath.class);
        when(mockedPath1.getPath()).thenReturn("/mock/device/lo");

        Map<String, Map<String, Variant<?>>> mockedDevice1ConnectionSetting = new HashMap<String, Map<String, Variant<?>>>();
        mockedDevice1ConnectionSetting.put("connection",
                Collections.singletonMap("uuid", new Variant<>("mock-uuid-123")));

        Settings mockedDevice1Settings = mock(Settings.class);
        when(mockedDevice1Settings.GetConnectionByUuid(eq("mock-uuid-123"))).thenReturn(mockedPath1);

        Connection mockedDevice1Connection = mock(Connection.class, RETURNS_SMART_NULLS);
        when(mockedDevice1Connection.GetSettings()).thenReturn(mockedDevice1ConnectionSetting);

        Properties mockedProperties1 = mock(Properties.class);
        when(mockedProperties1.Get(any(), any())).thenReturn("loopback");

        GetAppliedConnectionTuple mockedDevice1ConnectionTouple = mock(GetAppliedConnectionTuple.class);
        when(mockedDevice1ConnectionTouple.getConnection()).thenReturn(mockedDevice1ConnectionSetting);

        when(mockedDevice1.GetAppliedConnection(eq(new UInt32(0)))).thenReturn(mockedDevice1ConnectionTouple);

        when(mockedProperties1.Get(eq("org.freedesktop.NetworkManager.Device"), eq("DeviceType")))
                .thenReturn(new UInt32(14)); // 1 id Ethernet
        when(mockedProperties1.Get(eq("org.freedesktop.NetworkManager.Device"), eq("State")))
                .thenReturn(new UInt32(10));
        when(mockedProperties1.Get(eq("org.freedesktop.NetworkManager.Device"), eq("Interface"))).thenReturn("lo");

        when(this.mockedNetworkManager.GetAllDevices()).thenReturn(Arrays.asList(mockedPath1));

        when(this.mockedNetworkManager.GetDeviceByIpIface(eq("lo"))).thenReturn(mockedPath1);

        doReturn(mockedDevice1).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/mock/device/lo"), eq(Device.class));
        doReturn(mockedDevice1Settings).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/org/freedesktop/NetworkManager/Settings"), eq(Settings.class));
        doReturn(mockedProperties1).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/mock/device/lo"), eq(Properties.class));
        doReturn(mockedDevice1Connection).when(this.dbusConnection)
                .getRemoteObject(eq("org.freedesktop.NetworkManager"), eq("/mock/device/lo"), eq(Connection.class));
        doReturn(mockedDevice1Generic).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/mock/device/lo"), eq(Generic.class));
    }

    public void givenMockedDeviceConfiguredToBeUnsupported() throws DBusException {
        Device mockedDevice1 = mock(Device.class);
        when(mockedDevice1.getObjectPath()).thenReturn("/mock/device/eth0");
        DBusPath mockedPath1 = mock(DBusPath.class);
        when(mockedPath1.getPath()).thenReturn("/mock/device/eth0");
        Properties mockedProperties1 = mock(Properties.class);
        when(mockedProperties1.Get(eq("org.freedesktop.NetworkManager.Device"), eq("DeviceType")))
                .thenReturn(new UInt32(0)); // This field is set to unsupported
        when(mockedProperties1.Get(eq("org.freedesktop.NetworkManager.Device"), eq("State")))
                .thenReturn(new UInt32(10));
        when(mockedProperties1.Get(eq("org.freedesktop.NetworkManager.Device"), eq("Interface"))).thenReturn("eth0");

        when(this.mockedNetworkManager.GetAllDevices()).thenReturn(Arrays.asList(mockedPath1));

        when(this.mockedNetworkManager.GetDeviceByIpIface(eq("eth0"))).thenReturn(mockedPath1);

        doReturn(mockedDevice1).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/mock/device/eth0"), eq(Device.class));
        doReturn(mockedProperties1).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/mock/device/eth0"), eq(Properties.class));
    }

    public void givenNetworkConfigWhichEnablesEth0() {
        netConfig.put("net.interfaces", "eth0, ");
        netConfig.put("net.interface.eth0.config.dhcpClient4.enabled", false);
        netConfig.put("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledWAN");
        netConfig.put("net.interface.eth0.config.ip4.address", "192.168.0.12");
        netConfig.put("net.interface.eth0.config.ip4.prefix", (short) 25);
        netConfig.put("net.interface.eth0.config.ip4.dnsServers", "1.1.1.1");
        netConfig.put("net.interface.eth0.config.ip4.gateway", "192.168.0.1");
    }

    public void givenNetworkConfigWhichDisablesEth0() {
        netConfig.put("net.interfaces", "eth0, ");
        netConfig.put("net.interface.eth0.config.ip4.status", "netIPv4StatusDisabled");
    }

    public void givenNetworkConfigWhichDisablesLoopbackDevice() {
        netConfig.put("net.interfaces", "lo");
        netConfig.put("net.interface.lo.config.ip4.status", "netIPv4StatusDisabled");
    }

    public void whenGetDbusConnectionIsRun() {
        this.dbusConnectionInternal = this.instanceNMDbusConnector.getDbusConnection();
    }

    public void whenCloseConnectionIsRun() {
        this.instanceNMDbusConnector.closeConnection();
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
            internalStringList = this.instanceNMDbusConnector.getInterfaces();
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

    public void whenApplyWithNetowrkConfig(Map<String, Object> networkConfig) {
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

    public void thenVerifyNoExceptionIsThrown() {
        assertFalse(hasDBusExceptionBeenThrown);
        assertFalse(hasNoSuchElementExceptionThrown);
    }

    public void thenVerifyDBusExceptionIsThrown() {
        assertTrue(hasDBusExceptionBeenThrown);
    }

    public void thenNullPointerExceptionIsThrown() {
        assertTrue(hasNullPointerExceptionThrown);
    }

    public void thenVerifyNoSuchElementExceptionIsThrown() {
        assertTrue(hasNoSuchElementExceptionThrown);
    }

    public void thenGetDbusConnectionIsMockedConnection() {
        assertEquals(this.dbusConnection, this.dbusConnectionInternal);
    }

    public void thenVerifyConnectionClosed() {
        verify(this.dbusConnection, atLeastOnce()).disconnect();
    }

    public void thenVerifyCheckVersionIsRun() throws DBusException {
        verify(this.dbusConnection, atLeastOnce()).getRemoteObject(eq("org.freedesktop.NetworkManager"),
                eq("/org/freedesktop/NetworkManager"), any());
    }

    public void thenVerifyCheckPermissionsRan() {
        verify(this.mockedNetworkManager, atLeastOnce()).GetPermissions();
    }

    public void thenReturnedStringListIs(List<String> list) {
        assertEquals(this.internalStringList, list);
    }

}
