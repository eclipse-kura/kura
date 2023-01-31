package org.freedesktop.networkmanager.device;

import java.util.Map;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.NetworkManager.Device.Ppp")
public interface Ppp extends DBusInterface {




    public static class PropertiesChanged extends DBusSignal {

        private final Map<String, Variant<?>> properties;

        public PropertiesChanged(String _path, Map<String, Variant<?>> _properties) throws DBusException {
            super(_path, _properties);
            this.properties = _properties;
        }


        public Map<String, Variant<?>> getProperties() {
            return properties;
        }


    }
}