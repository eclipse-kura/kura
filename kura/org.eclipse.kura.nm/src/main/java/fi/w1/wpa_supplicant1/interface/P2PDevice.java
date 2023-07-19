package fi.w1.wpa_supplicant1.interface;

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
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt64;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("fi.w1.wpa_supplicant1.Interface.P2PDevice")
@DBusProperty(name = "P2PDeviceConfig", type = P2PDevice.PropertyP2PDeviceConfigType.class, access = Access.READ_WRITE)
@DBusProperty(name = "Peers", type = P2PDevice.PropertyPeersType.class, access = Access.READ)
@DBusProperty(name = "Role", type = String.class, access = Access.READ)
@DBusProperty(name = "Group", type = DBusPath.class, access = Access.READ)
@DBusProperty(name = "PeerGO", type = DBusPath.class, access = Access.READ)
@DBusProperty(name = "PersistentGroups", type = P2PDevice.PropertyPersistentGroupsType.class, access = Access.READ)
public interface P2PDevice extends DBusInterface {


    public void Find(Map<String, Variant<?>> args);
    public void StopFind();
    public void Listen(int timeout);
    public void ExtendedListen(Map<String, Variant<?>> args);
    public void PresenceRequest(Map<String, Variant<?>> args);
    public void ProvisionDiscoveryRequest(DBusPath peer, String configMethod);
    public String Connect(Map<String, Variant<?>> args);
    public void GroupAdd(Map<String, Variant<?>> args);
    public void Cancel();
    public void Invite(Map<String, Variant<?>> args);
    public void Disconnect();
    public void RejectPeer(DBusPath peer);
    public void RemoveClient(Map<String, Variant<?>> args);
    public void Flush();
    public void AddService(Map<String, Variant<?>> args);
    public void DeleteService(Map<String, Variant<?>> args);
    public void FlushService();
    public UInt64 ServiceDiscoveryRequest(Map<String, Variant<?>> args);
    public void ServiceDiscoveryResponse(Map<String, Variant<?>> args);
    public void ServiceDiscoveryCancelRequest(UInt64 args);
    public void ServiceUpdate();
    public void ServiceDiscoveryExternal(int arg);
    public DBusPath AddPersistentGroup(Map<String, Variant<?>> args);
    public void RemovePersistentGroup(DBusPath path);
    public void RemoveAllPersistentGroups();


    public static class DeviceFound extends DBusSignal {

        private final DBusPath path;

        public DeviceFound(String _path, DBusPath _path) throws DBusException {
            super(_path, _path);
            this.path = _path;
        }


        public DBusPath getPath() {
            return path;
        }


    }

    public static class DeviceFoundProperties extends DBusSignal {

        private final DBusPath path;
        private final Map<String, Variant<?>> properties;

        public DeviceFoundProperties(String _path, DBusPath _path, Map<String, Variant<?>> _properties) throws DBusException {
            super(_path, _path, _properties);
            this.path = _path;
            this.properties = _properties;
        }


        public DBusPath getPath() {
            return path;
        }

        public Map<String, Variant<?>> getProperties() {
            return properties;
        }


    }

    public static class DeviceLost extends DBusSignal {

        private final DBusPath path;

        public DeviceLost(String _path, DBusPath _path) throws DBusException {
            super(_path, _path);
            this.path = _path;
        }


        public DBusPath getPath() {
            return path;
        }


    }

    public static class ProvisionDiscoveryRequestDisplayPin extends DBusSignal {

        private final DBusPath peerObject;
        private final String pin;

        public ProvisionDiscoveryRequestDisplayPin(String _path, DBusPath _peerObject, String _pin) throws DBusException {
            super(_path, _peerObject, _pin);
            this.peerObject = _peerObject;
            this.pin = _pin;
        }


        public DBusPath getPeerObject() {
            return peerObject;
        }

        public String getPin() {
            return pin;
        }


    }

    public static class ProvisionDiscoveryResponseDisplayPin extends DBusSignal {

        private final DBusPath peerObject;
        private final String pin;

        public ProvisionDiscoveryResponseDisplayPin(String _path, DBusPath _peerObject, String _pin) throws DBusException {
            super(_path, _peerObject, _pin);
            this.peerObject = _peerObject;
            this.pin = _pin;
        }


        public DBusPath getPeerObject() {
            return peerObject;
        }

        public String getPin() {
            return pin;
        }


    }

    public static class ProvisionDiscoveryRequestEnterPin extends DBusSignal {

        private final DBusPath peerObject;

        public ProvisionDiscoveryRequestEnterPin(String _path, DBusPath _peerObject) throws DBusException {
            super(_path, _peerObject);
            this.peerObject = _peerObject;
        }


        public DBusPath getPeerObject() {
            return peerObject;
        }


    }

    public static class ProvisionDiscoveryResponseEnterPin extends DBusSignal {

        private final DBusPath peerObject;

        public ProvisionDiscoveryResponseEnterPin(String _path, DBusPath _peerObject) throws DBusException {
            super(_path, _peerObject);
            this.peerObject = _peerObject;
        }


        public DBusPath getPeerObject() {
            return peerObject;
        }


    }

    public static class ProvisionDiscoveryPBCRequest extends DBusSignal {

        private final DBusPath peerObject;

        public ProvisionDiscoveryPBCRequest(String _path, DBusPath _peerObject) throws DBusException {
            super(_path, _peerObject);
            this.peerObject = _peerObject;
        }


        public DBusPath getPeerObject() {
            return peerObject;
        }


    }

    public static class ProvisionDiscoveryPBCResponse extends DBusSignal {

        private final DBusPath peerObject;

        public ProvisionDiscoveryPBCResponse(String _path, DBusPath _peerObject) throws DBusException {
            super(_path, _peerObject);
            this.peerObject = _peerObject;
        }


        public DBusPath getPeerObject() {
            return peerObject;
        }


    }

    public static class ProvisionDiscoveryFailure extends DBusSignal {

        private final DBusPath peerObject;
        private final int status;

        public ProvisionDiscoveryFailure(String _path, DBusPath _peerObject, int _status) throws DBusException {
            super(_path, _peerObject, _status);
            this.peerObject = _peerObject;
            this.status = _status;
        }


        public DBusPath getPeerObject() {
            return peerObject;
        }

        public int getStatus() {
            return status;
        }


    }

    public static class GroupStarted extends DBusSignal {

        private final Map<String, Variant<?>> properties;

        public GroupStarted(String _path, Map<String, Variant<?>> _properties) throws DBusException {
            super(_path, _properties);
            this.properties = _properties;
        }


        public Map<String, Variant<?>> getProperties() {
            return properties;
        }


    }

    public static class GroupFormationFailure extends DBusSignal {

        private final String reason;

        public GroupFormationFailure(String _path, String _reason) throws DBusException {
            super(_path, _reason);
            this.reason = _reason;
        }


        public String getReason() {
            return reason;
        }


    }

    public static class GONegotiationSuccess extends DBusSignal {

        private final Map<String, Variant<?>> properties;

        public GONegotiationSuccess(String _path, Map<String, Variant<?>> _properties) throws DBusException {
            super(_path, _properties);
            this.properties = _properties;
        }


        public Map<String, Variant<?>> getProperties() {
            return properties;
        }


    }

    public static class GONegotiationFailure extends DBusSignal {

        private final Map<String, Variant<?>> properties;

        public GONegotiationFailure(String _path, Map<String, Variant<?>> _properties) throws DBusException {
            super(_path, _properties);
            this.properties = _properties;
        }


        public Map<String, Variant<?>> getProperties() {
            return properties;
        }


    }

    public static class GONegotiationRequest extends DBusSignal {

        private final DBusPath path;
        private final UInt16 devPasswdId;
        private final byte deviceGoIntent;

        public GONegotiationRequest(String _path, DBusPath _path, UInt16 _devPasswdId, byte _deviceGoIntent) throws DBusException {
            super(_path, _path, _devPasswdId, _deviceGoIntent);
            this.path = _path;
            this.devPasswdId = _devPasswdId;
            this.deviceGoIntent = _deviceGoIntent;
        }


        public DBusPath getPath() {
            return path;
        }

        public UInt16 getDevPasswdId() {
            return devPasswdId;
        }

        public byte getDeviceGoIntent() {
            return deviceGoIntent;
        }


    }

    public static class InvitationResult extends DBusSignal {

        private final Map<String, Variant<?>> inviteResult;

        public InvitationResult(String _path, Map<String, Variant<?>> _inviteResult) throws DBusException {
            super(_path, _inviteResult);
            this.inviteResult = _inviteResult;
        }


        public Map<String, Variant<?>> getInviteResult() {
            return inviteResult;
        }


    }

    public static class GroupFinished extends DBusSignal {

        private final Map<String, Variant<?>> properties;

        public GroupFinished(String _path, Map<String, Variant<?>> _properties) throws DBusException {
            super(_path, _properties);
            this.properties = _properties;
        }


        public Map<String, Variant<?>> getProperties() {
            return properties;
        }


    }

    public static class ServiceDiscoveryRequest extends DBusSignal {

        private final Map<String, Variant<?>> sdRequest;

        public ServiceDiscoveryRequest(String _path, Map<String, Variant<?>> _sdRequest) throws DBusException {
            super(_path, _sdRequest);
            this.sdRequest = _sdRequest;
        }


        public Map<String, Variant<?>> getSdRequest() {
            return sdRequest;
        }


    }

    public static class ServiceDiscoveryResponse extends DBusSignal {

        private final Map<String, Variant<?>> sdResponse;

        public ServiceDiscoveryResponse(String _path, Map<String, Variant<?>> _sdResponse) throws DBusException {
            super(_path, _sdResponse);
            this.sdResponse = _sdResponse;
        }


        public Map<String, Variant<?>> getSdResponse() {
            return sdResponse;
        }


    }

    public static class PersistentGroupAdded extends DBusSignal {

        private final DBusPath path;
        private final Map<String, Variant<?>> properties;

        public PersistentGroupAdded(String _path, DBusPath _path, Map<String, Variant<?>> _properties) throws DBusException {
            super(_path, _path, _properties);
            this.path = _path;
            this.properties = _properties;
        }


        public DBusPath getPath() {
            return path;
        }

        public Map<String, Variant<?>> getProperties() {
            return properties;
        }


    }

    public static class PersistentGroupRemoved extends DBusSignal {

        private final DBusPath path;

        public PersistentGroupRemoved(String _path, DBusPath _path) throws DBusException {
            super(_path, _path);
            this.path = _path;
        }


        public DBusPath getPath() {
            return path;
        }


    }

    public static class WpsFailed extends DBusSignal {

        private final String name;
        private final Map<String, Variant<?>> args;

        public WpsFailed(String _path, String _name, Map<String, Variant<?>> _args) throws DBusException {
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

    public static class InvitationReceived extends DBusSignal {

        private final Map<String, Variant<?>> properties;

        public InvitationReceived(String _path, Map<String, Variant<?>> _properties) throws DBusException {
            super(_path, _properties);
            this.properties = _properties;
        }


        public Map<String, Variant<?>> getProperties() {
            return properties;
        }


    }

    public static interface PropertyP2PDeviceConfigType extends TypeRef<Map<String, Variant>> {




    }

    public static interface PropertyPeersType extends TypeRef<List<DBusPath>> {




    }

    public static interface PropertyPersistentGroupsType extends TypeRef<List<DBusPath>> {




    }
}