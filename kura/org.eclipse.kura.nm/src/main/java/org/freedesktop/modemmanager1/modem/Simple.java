package org.freedesktop.modemmanager1.modem;

import java.util.Map;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.ModemManager1.Modem.Simple")
public interface Simple extends DBusInterface {


    public DBusPath Connect(Map<String, Variant<?>> properties);
    public void Disconnect(DBusPath bearer);
    public Map<String, Variant<?>> GetStatus();

}