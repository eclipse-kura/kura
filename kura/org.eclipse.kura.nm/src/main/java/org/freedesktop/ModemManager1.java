package org.freedesktop;

import java.util.Map;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusProperty(name = "Version", type = String.class, access = Access.READ)
public interface ModemManager1 extends DBusInterface {


    public void ScanDevices();
    public void SetLogging(String level);
    public void ReportKernelEvent(Map<String, Variant<?>> properties);
    public void InhibitDevice(String uid, boolean inhibit);

}