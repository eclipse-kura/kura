package fi.w1.wpa_supplicant1.interface;

import java.util.List;
import java.util.Map;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("fi.w1.wpa_supplicant1.Interface.WPS")
@DBusProperty(name = "ProcessCredentials", type = Boolean.class, access = Access.READ_WRITE)
@DBusProperty(name = "ConfigMethods", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "DeviceName", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "Manufacturer", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "ModelName", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "ModelNumber", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "SerialNumber", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "DeviceType", type = WPS.PropertyDeviceTypeType.class, access = Access.READ_WRITE)
public interface WPS extends DBusInterface {


    public Map<String, Variant<?>> Start(Map<String, Variant<?>> args);
    public void Cancel();


    public static class Event extends DBusSignal {

        private final String name;
        private final Map<String, Variant<?>> args;

        public Event(String _path, String _name, Map<String, Variant<?>> _args) throws DBusException {
            super(_path, _name, _args);
            this.name = _name;
            this.args = _args;
        }


        public String getName() {
            return name;
        }

        public Map<String, Variant<?>> getArgs() {
            return args;
        }


    }

    public static class Credentials extends DBusSignal {

        private final Map<String, Variant<?>> credentials;

        public Credentials(String _path, Map<String, Variant<?>> _credentials) throws DBusException {
            super(_path, _credentials);
            this.credentials = _credentials;
        }


        public Map<String, Variant<?>> getCredentials() {
            return credentials;
        }


    }

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

    public static interface PropertyDeviceTypeType extends TypeRef<List<Byte>> {




    }
}