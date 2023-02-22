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
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.ModemManager1.Modem.Voice")
@DBusProperty(name = "Calls", type = Voice.PropertyCallsType.class, access = Access.READ)
@DBusProperty(name = "EmergencyOnly", type = Boolean.class, access = Access.READ)
public interface Voice extends DBusInterface {


    public List<DBusPath> ListCalls();
    public void DeleteCall(DBusPath path);
    public DBusPath CreateCall(Map<String, Variant<?>> properties);
    public void HoldAndAccept();
    public void HangupAndAccept();
    public void HangupAll();
    public void Transfer();
    public void CallWaitingSetup(boolean enable);
    public boolean CallWaitingQuery();


    public static class CallAdded extends DBusSignal {

        private final DBusPath path;

        public CallAdded(String _path, DBusPath _path) throws DBusException {
            super(_path, _path);
            this.path = _path;
        }


        public DBusPath getPath() {
            return path;
        }


    }

    public static class CallDeleted extends DBusSignal {

        private final DBusPath path;

        public CallDeleted(String _path, DBusPath _path) throws DBusException {
            super(_path, _path);
            this.path = _path;
        }


        public DBusPath getPath() {
            return path;
        }


    }

    public static interface PropertyCallsType extends TypeRef<List<DBusPath>> {




    }
}