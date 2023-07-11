package org.eclipse.kura.nm;

import java.util.Map;

import org.eclipse.kura.nm.enums.NMDeviceState;
import org.freedesktop.NetworkManager;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.networkmanager.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkManagerDbusWrapper {

    private static final Logger logger = LoggerFactory.getLogger(NetworkManagerDbusWrapper.class);

    private static final String NM_BUS_NAME = "org.freedesktop.NetworkManager";
    private static final String NM_BUS_PATH = "/org/freedesktop/NetworkManager";
    private static final String NM_DEVICE_BUS_NAME = "org.freedesktop.NetworkManager.Device";
    private static final String NM_GENERIC_DEVICE_BUS_NAME = "org.freedesktop.NetworkManager.Device.Generic";
    private static final String NM_SETTINGS_BUS_PATH = "/org/freedesktop/NetworkManager/Settings";

    private static final String NM_PROPERTY_VERSION = "Version";
    private static final String NM_DEVICE_PROPERTY_INTERFACE = "Interface";
    private static final String NM_DEVICE_PROPERTY_MANAGED = "Managed";
    private static final String NM_DEVICE_PROPERTY_DEVICETYPE = "DeviceType";
    private static final String NM_DEVICE_PROPERTY_STATE = "State";
    private static final String NM_SETTING_CONNECTION_KEY = "connection";
    private static final String NM_DEVICE_GENERIC_PROPERTY_TYPEDESCRIPTION = "TypeDescription";

    private DBusConnection dbusConnection;
    private NetworkManager networkManager;

    protected NetworkManagerDbusWrapper(DBusConnection dbusConnection) throws DBusException {
        this.dbusConnection = dbusConnection;
        this.networkManager = dbusConnection.getRemoteObject(NM_BUS_NAME, NM_BUS_PATH, NetworkManager.class);
    }

    protected String getVersion() throws DBusException {
        Properties nmProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, NM_BUS_PATH, Properties.class);
        return nmProperties.Get(NM_BUS_NAME, NM_PROPERTY_VERSION);
    }

    protected Map<String, String> getPermissions() {
        return this.networkManager.GetPermissions();
    }

    protected String getDeviceInterface(Device device) throws DBusException {
        Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                Properties.class);
        return deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_INTERFACE);
    }

    protected NMDeviceState getDeviceState(Device device) throws DBusException {
        Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                Properties.class);
        return NMDeviceState.fromUInt32(deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_STATE));
    }

    protected Boolean isDeviceManaged(Device device) throws DBusException {
        Properties deviceProperties = this.dbusConnection.getRemoteObject(NM_BUS_NAME, device.getObjectPath(),
                Properties.class);
        return deviceProperties.Get(NM_DEVICE_BUS_NAME, NM_DEVICE_PROPERTY_MANAGED);
    }
}