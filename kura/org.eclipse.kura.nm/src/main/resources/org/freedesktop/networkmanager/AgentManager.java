package org.freedesktop.networkmanager;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt32;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.NetworkManager.AgentManager")
public interface AgentManager extends DBusInterface {


    public void Register(String identifier);
    public void RegisterWithCapabilities(String identifier, UInt32 capabilities);
    public void Unregister();

}