package org.eclipse.kura.nm;

import java.util.Map;

import org.freedesktop.NetworkManager;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkManagerDbusWrapper {

    private static final Logger logger = LoggerFactory.getLogger(NetworkManagerDbusWrapper.class);

    private static final String NM_BUS_NAME = "org.freedesktop.NetworkManager";
    private static final String NM_BUS_PATH = "/org/freedesktop/NetworkManager";

    private static final String NM_PROPERTY_VERSION = "Version";

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
}