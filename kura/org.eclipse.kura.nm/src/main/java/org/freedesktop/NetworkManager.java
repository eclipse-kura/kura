/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.freedesktop;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.TypeRef;
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
@DBusProperty(name = "Devices", type = NetworkManager.PropertyDevicesType.class, access = Access.READ)
@DBusProperty(name = "AllDevices", type = NetworkManager.PropertyAllDevicesType.class, access = Access.READ)
@DBusProperty(name = "Checkpoints", type = NetworkManager.PropertyCheckpointsType.class, access = Access.READ)
@DBusProperty(name = "NetworkingEnabled", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "WirelessEnabled", type = Boolean.class, access = Access.READ_WRITE)
@DBusProperty(name = "WirelessHardwareEnabled", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "WwanEnabled", type = Boolean.class, access = Access.READ_WRITE)
@DBusProperty(name = "WwanHardwareEnabled", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "WimaxEnabled", type = Boolean.class, access = Access.READ_WRITE)
@DBusProperty(name = "WimaxHardwareEnabled", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "ActiveConnections", type = NetworkManager.PropertyActiveConnectionsType.class, access = Access.READ)
@DBusProperty(name = "PrimaryConnection", type = DBusPath.class, access = Access.READ)
@DBusProperty(name = "PrimaryConnectionType", type = String.class, access = Access.READ)
@DBusProperty(name = "Metered", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "ActivatingConnection", type = DBusPath.class, access = Access.READ)
@DBusProperty(name = "Startup", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "Version", type = String.class, access = Access.READ)
@DBusProperty(name = "Capabilities", type = NetworkManager.PropertyCapabilitiesType.class, access = Access.READ)
@DBusProperty(name = "State", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Connectivity", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "ConnectivityCheckAvailable", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "ConnectivityCheckEnabled", type = Boolean.class, access = Access.READ_WRITE)
@DBusProperty(name = "ConnectivityCheckUri", type = String.class, access = Access.READ)
@DBusProperty(name = "GlobalDnsConfiguration", type = NetworkManager.PropertyGlobalDnsConfigurationType.class, access = Access.READ_WRITE)
public interface NetworkManager extends DBusInterface {

    public void Reload(UInt32 flags);

    public List<DBusPath> GetDevices();

    public List<DBusPath> GetAllDevices();

    public DBusPath GetDeviceByIpIface(String iface);

    public DBusPath ActivateConnection(DBusPath connection, DBusPath device, DBusPath specificObject);

    public AddAndActivateConnectionTuple AddAndActivateConnection(Map<String, Map<String, Variant<?>>> connection,
            DBusPath device, DBusPath specificObject);

    public AddAndActivateConnection2Tuple AddAndActivateConnection2(Map<String, Map<String, Variant<?>>> connection,
            DBusPath device, DBusPath specificObject, Map<String, Variant<?>> options);

    public void DeactivateConnection(DBusPath activeConnection);

    public void Sleep(boolean sleep);

    public void Enable(boolean enable);

    public Map<String, String> GetPermissions();

    public void SetLogging(String level, String domains);

    public GetLoggingTuple GetLogging();

    public UInt32 CheckConnectivity();

    public UInt32 state();

    public DBusPath CheckpointCreate(List<DBusPath> devices, UInt32 rollbackTimeout, UInt32 flags);

    public void CheckpointDestroy(DBusPath checkpoint);

    public Map<String, UInt32> CheckpointRollback(DBusPath checkpoint);

    public void CheckpointAdjustRollbackTimeout(DBusPath checkpoint, UInt32 addTimeout);

    public static interface PropertyDevicesType extends TypeRef<List<DBusPath>> {

    }

    public static interface PropertyAllDevicesType extends TypeRef<List<DBusPath>> {

    }

    public static interface PropertyCheckpointsType extends TypeRef<List<DBusPath>> {

    }

    public static interface PropertyActiveConnectionsType extends TypeRef<List<DBusPath>> {

    }

    public static interface PropertyCapabilitiesType extends TypeRef<List<UInt32>> {

    }

    public static class StateChanged extends DBusSignal {

        private final UInt32 state;

        public StateChanged(String _path, UInt32 _state) throws DBusException {
            super(_path, _state);
            this.state = _state;
        }

        public UInt32 getState() {
            return this.state;
        }

    }

    public static interface PropertyGlobalDnsConfigurationType extends TypeRef<Map<String, Variant>> {

    }

    public static class PropertiesChanged extends DBusSignal {

        private final Map<String, Variant<?>> properties;

        public PropertiesChanged(String _path, Map<String, Variant<?>> _properties) throws DBusException {
            super(_path, _properties);
            this.properties = _properties;
        }

        public Map<String, Variant<?>> getProperties() {
            return this.properties;
        }

    }

    public static class DeviceAdded extends DBusSignal {

        private final DBusPath devicePath;

        public DeviceAdded(String _path, DBusPath _devicePath) throws DBusException {
            super(_path, _devicePath);
            this.devicePath = _devicePath;
        }

        public DBusPath getDevicePath() {
            return this.devicePath;
        }

    }

    public static class DeviceRemoved extends DBusSignal {

        private final DBusPath devicePath;

        public DeviceRemoved(String _path, DBusPath _devicePath) throws DBusException {
            super(_path, _devicePath);
            this.devicePath = _devicePath;
        }

        public DBusPath getDevicePath() {
            return this.devicePath;
        }

    }
}
