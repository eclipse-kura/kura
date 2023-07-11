package org.eclipse.kura.nm;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModemManagerDbusWrapper {

    private static final Logger logger = LoggerFactory.getLogger(ModemManagerDbusWrapper.class);

    private static final String MM_BUS_NAME = "org.freedesktop.ModemManager1";
    private static final String MM_BUS_PATH = "/org/freedesktop/ModemManager1";
    private static final String MM_MODEM_NAME = "org.freedesktop.ModemManager1.Modem";
    private static final String MM_SIM_NAME = "org.freedesktop.ModemManager1.Sim";
    private static final String MM_MODEM_PROPERTY_STATE = "State";
    private static final String MM_LOCATION_BUS_NAME = "org.freedesktop.ModemManager1.Modem.Location";

    private DBusConnection dbusConnection;

    protected ModemManagerDbusWrapper(DBusConnection dbusConnection) {
        this.dbusConnection = dbusConnection;
    }

}
