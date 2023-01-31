package org.freedesktop.networkmanager.device;

import java.util.List;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt32;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.NetworkManager.Device.WireGuard")
@DBusProperty(name = "PublicKey", type = WireGuard.PropertyPublicKeyType.class, access = Access.READ)
@DBusProperty(name = "ListenPort", type = UInt16.class, access = Access.READ)
@DBusProperty(name = "FwMark", type = UInt32.class, access = Access.READ)
public interface WireGuard extends DBusInterface {




    public static interface PropertyPublicKeyType extends TypeRef<List<Byte>> {




    }
}