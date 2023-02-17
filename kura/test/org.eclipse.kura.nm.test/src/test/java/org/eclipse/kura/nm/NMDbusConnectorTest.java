package org.eclipse.kura.nm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.freedesktop.NetworkManager;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.networkmanager.Device;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class NMDbusConnectorTest {
	DBusConnection dbusConnection = Mockito.mock(DBusConnection.class, Mockito.RETURNS_SMART_NULLS);
	NetworkManager mockedNetworkManager = Mockito.mock(NetworkManager.class);
	NMDbusConnector instanceNMDbusConnector;
	DBusConnection dbusConnectionInternal;
	
	Boolean hasDBusExceptionBeenThrown = false;
	
	List<String> internalStringList;
	
	@Before
	public void setUpPersistantMocks() throws DBusException {
		givenBasicMockedDbusConnector();
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
		thenReturnedStringListIs(Arrays.asList("mockedDevice1","mockedDevice2"));
	}
	
	@Test
	public void applyShouldDoNothingWithNoCache() throws DBusException {
		givenMockedDevices();
		
		whenApply();
		
		thenVerifyNoExceptionIsThrown();
		//check if nothing happened
	}

	public void givenBasicMockedDbusConnector() throws DBusException {
		when(dbusConnection.getRemoteObject(eq("org.freedesktop.NetworkManager"), eq("/org/freedesktop/NetworkManager"), any()))
					.thenReturn(mockedNetworkManager);
		
		this.instanceNMDbusConnector = new  NMDbusConnector(this.dbusConnection){
			
			protected static synchronized NMDbusConnector getInstance(DBusConnection dbusConnection) throws DBusException {
				instance = new NMDbusConnector(dbusConnection);
				return instance;
		    }
		};
	
		NMDbusConnector.getInstance(this.dbusConnection);
	}
	
	public void givenMockedPermissions() {
		
		Map<String, String> tempPerms = new HashMap<>();
		
		tempPerms.put("test1", "testVal1");
		tempPerms.put("test2", "testVal2");
		tempPerms.put("test3", "testVal3");
		
		when(mockedNetworkManager.GetPermissions()).thenReturn(tempPerms);
		
	}
	
	public void givenMockedVersion() throws DBusException {
		
		Properties mockProps = Mockito.mock(org.freedesktop.dbus.interfaces.Properties.class);
		when(mockProps.Get(eq("org.freedesktop.NetworkManager"), eq("Version"))).thenReturn("Mock-Version");
		
		Mockito.doReturn(mockProps).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"), eq("/org/freedesktop/NetworkManager"),  eq(Properties.class));
	}
	
	public void givenMockedDevices() throws DBusException {
		
		Device mockedDevice1 = mock(Device.class);
		when(mockedDevice1.getObjectPath()).thenReturn("/mock/device/path1");
		DBusPath mockedPath1 = mock(DBusPath.class);
		when(mockedPath1.getPath()).thenReturn("/mock/device/path1");
		Properties mockedProperties1 = mock(Properties.class);
		when(mockedProperties1.Get(eq("org.freedesktop.NetworkManager.Device"), eq("DeviceType"))).thenReturn(new UInt32(1));
		when(mockedProperties1.Get(eq("org.freedesktop.NetworkManager.Device"), eq("Interface"))).thenReturn("mockedDevice1");
		
		Device mockedDevice2 = mock(Device.class);
		when(mockedDevice2.getObjectPath()).thenReturn("/mock/device/path2");
		DBusPath mockedPath2 = mock(DBusPath.class);
		when(mockedPath2.getPath()).thenReturn("/mock/device/path2");
		Properties mockedProperties2 = mock(Properties.class);
		when(mockedProperties2.Get(eq("org.freedesktop.NetworkManager.Device"), eq("DeviceType"))).thenReturn(new UInt32(1));
		when(mockedProperties2.Get(eq("org.freedesktop.NetworkManager.Device"), eq("Interface"))).thenReturn("mockedDevice2");
		
		
		when(this.mockedNetworkManager.GetAllDevices()).thenReturn(Arrays.asList(mockedPath1, mockedPath2));
		
		Mockito.doReturn(mockedDevice1).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"), eq("/mock/device/path1"),  eq(Device.class));
		Mockito.doReturn(mockedProperties1).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"), eq("/mock/device/path1"),  eq(Properties.class));

		Mockito.doReturn(mockedDevice2).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"), eq("/mock/device/path2"),  eq(Device.class));
		Mockito.doReturn(mockedProperties2).when(this.dbusConnection).getRemoteObject(eq("org.freedesktop.NetworkManager"), eq("/mock/device/path2"),  eq(Properties.class));
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
	
	public void whenApplyWithNetowrkConfig(Map<String,Object> networkConfig) {
		try {
			this.instanceNMDbusConnector.apply(networkConfig);
		} catch (DBusException e) {
			hasDBusExceptionBeenThrown = true;
		}
	}
	
	public void thenVerifyNoExceptionIsThrown() {
		assertFalse(hasDBusExceptionBeenThrown);
	}
	
	public void thenGetDbusConnectionIsMockedConnection() {
		assertEquals(this.dbusConnection, this.dbusConnectionInternal);
	}
	
	public void thenVerifyConnectionClosed() {
		verify(this.dbusConnection, Mockito.atLeastOnce()).disconnect();
	}
	
	public void thenVerifyCheckVersionIsRun() throws DBusException {
		verify(this.dbusConnection, Mockito.atLeastOnce()).getRemoteObject(eq("org.freedesktop.NetworkManager"), eq("/org/freedesktop/NetworkManager"), Mockito.any());
	}
	
	public void thenVerifyCheckPermissionsRan() {
		verify(this.mockedNetworkManager, Mockito.atLeastOnce()).GetPermissions();
	}
	
	public void thenReturnedStringListIs(List<String> list) {
		assertEquals(this.internalStringList, list);
	}

}
