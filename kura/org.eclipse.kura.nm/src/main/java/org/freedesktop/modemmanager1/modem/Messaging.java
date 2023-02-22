package org.freedesktop.modemmanager1.modem;

import java.util.List;
import java.util.Map;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.ModemManager1.Modem.Messaging")
@DBusProperty(name = "Messages", type = Messaging.PropertyMessagesType.class, access = Access.READ)
@DBusProperty(name = "SupportedStorages", type = Messaging.PropertySupportedStoragesType.class, access = Access.READ)
@DBusProperty(name = "DefaultStorage", type = UInt32.class, access = Access.READ)
public interface Messaging extends DBusInterface {


    public List<DBusPath> List();
    public void Delete(DBusPath path);
    public DBusPath Create(Map<String, Variant<?>> properties);


    public static class Added extends DBusSignal {

        private final DBusPath path;
        private final boolean received;

        public Added(String _path, DBusPath _path, boolean _received) throws DBusException {
            super(_path, _path, _received);
            this.path = _path;
            this.received = _received;
        }


        public DBusPath getPath() {
            return path;
        }

        public boolean getReceived() {
            return received;
        }


    }

    public static class Deleted extends DBusSignal {

        private final DBusPath path;

        public Deleted(String _path, DBusPath _path) throws DBusException {
            super(_path, _path);
            this.path = _path;
        }


        public DBusPath getPath() {
            return path;
        }


    }

    public static interface PropertyMessagesType extends TypeRef<List<DBusPath>> {




    }

    public static interface PropertySupportedStoragesType extends TypeRef<List<UInt32>> {




    }
}