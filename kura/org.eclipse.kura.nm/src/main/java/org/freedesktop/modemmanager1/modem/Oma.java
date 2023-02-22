package org.freedesktop.modemmanager1.modem;

import java.util.List;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.modemmanager1.modem.PropertyPendingNetworkInitiatedSessionsStruct;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.ModemManager1.Modem.Oma")
@DBusProperty(name = "Features", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "PendingNetworkInitiatedSessions", type = Oma.PropertyPendingNetworkInitiatedSessionsType.class, access = Access.READ)
@DBusProperty(name = "SessionType", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "SessionState", type = Integer.class, access = Access.READ)
public interface Oma extends DBusInterface {


    public void Setup(UInt32 features);
    public void StartClientInitiatedSession(UInt32 sessionType);
    public void AcceptNetworkInitiatedSession(UInt32 sessionId, boolean accept);
    public void CancelSession();


    public static interface PropertyPendingNetworkInitiatedSessionsType extends TypeRef<List<PropertyPendingNetworkInitiatedSessionsStruct>> {




    }

    public static class SessionStateChanged extends DBusSignal {

        private final int oldSessionState;
        private final int newSessionState;
        private final UInt32 sessionStateFailedReason;

        public SessionStateChanged(String _path, int _oldSessionState, int _newSessionState, UInt32 _sessionStateFailedReason) throws DBusException {
            super(_path, _oldSessionState, _newSessionState, _sessionStateFailedReason);
            this.oldSessionState = _oldSessionState;
            this.newSessionState = _newSessionState;
            this.sessionStateFailedReason = _sessionStateFailedReason;
        }


        public int getOldSessionState() {
            return oldSessionState;
        }

        public int getNewSessionState() {
            return newSessionState;
        }

        public UInt32 getSessionStateFailedReason() {
            return sessionStateFailedReason;
        }


    }
}