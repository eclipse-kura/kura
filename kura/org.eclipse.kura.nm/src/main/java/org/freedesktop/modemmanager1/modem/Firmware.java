package org.freedesktop.modemmanager1.modem;

import java.util.List;
import java.util.Map;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.modemmanager1.modem.PropertyUpdateSettingsStruct;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.ModemManager1.Modem.Firmware")
@DBusProperty(name = "UpdateSettings", type = PropertyUpdateSettingsStruct.class, access = Access.READ)
public interface Firmware extends DBusInterface {


    public ListTuple List();
    public void Select(String uniqueid);

}