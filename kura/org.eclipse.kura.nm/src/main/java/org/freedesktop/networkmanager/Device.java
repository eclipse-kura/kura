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
package org.freedesktop.networkmanager;

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
import org.freedesktop.dbus.types.UInt64;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.NetworkManager.Device")
@DBusProperty(name = "Udi", type = String.class, access = Access.READ)
@DBusProperty(name = "Path", type = String.class, access = Access.READ)
@DBusProperty(name = "Interface", type = String.class, access = Access.READ)
@DBusProperty(name = "IpInterface", type = String.class, access = Access.READ)
@DBusProperty(name = "Driver", type = String.class, access = Access.READ)
@DBusProperty(name = "DriverVersion", type = String.class, access = Access.READ)
@DBusProperty(name = "FirmwareVersion", type = String.class, access = Access.READ)
@DBusProperty(name = "Capabilities", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Ip4Address", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "State", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "StateReason", type = PropertyStateReasonStruct.class, access = Access.READ)
@DBusProperty(name = "ActiveConnection", type = DBusPath.class, access = Access.READ)
@DBusProperty(name = "Ip4Config", type = DBusPath.class, access = Access.READ)
@DBusProperty(name = "Dhcp4Config", type = DBusPath.class, access = Access.READ)
@DBusProperty(name = "Ip6Config", type = DBusPath.class, access = Access.READ)
@DBusProperty(name = "Dhcp6Config", type = DBusPath.class, access = Access.READ)
@DBusProperty(name = "Managed", type = Boolean.class, access = Access.READ_WRITE)
@DBusProperty(name = "Autoconnect", type = Boolean.class, access = Access.READ_WRITE)
@DBusProperty(name = "FirmwareMissing", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "NmPluginMissing", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "DeviceType", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "AvailableConnections", type = Device.PropertyAvailableConnectionsType.class, access = Access.READ)
@DBusProperty(name = "PhysicalPortId", type = String.class, access = Access.READ)
@DBusProperty(name = "Mtu", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Metered", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "LldpNeighbors", type = Device.PropertyLldpNeighborsType.class, access = Access.READ)
@DBusProperty(name = "Real", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "Ip4Connectivity", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Ip6Connectivity", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "InterfaceFlags", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "HwAddress", type = String.class, access = Access.READ)
public interface Device extends DBusInterface {

    public void Reapply(Map<String, Map<String, Variant<?>>> connection, UInt64 versionId, UInt32 flags);

    public GetAppliedConnectionTuple GetAppliedConnection(UInt32 flags);

    public void Disconnect();

    public void Delete();

    public static interface PropertyAvailableConnectionsType extends TypeRef<List<DBusPath>> {

    }

    public static interface PropertyLldpNeighborsType extends TypeRef<List<Map<String, Variant>>> {

    }

    public static class StateChanged extends DBusSignal {

        private final UInt32 newState;
        private final UInt32 oldState;
        private final UInt32 reason;

        public StateChanged(String _path, UInt32 _newState, UInt32 _oldState, UInt32 _reason) throws DBusException {
            super(_path, _newState, _oldState, _reason);
            this.newState = _newState;
            this.oldState = _oldState;
            this.reason = _reason;
        }

        public UInt32 getNewState() {
            return this.newState;
        }

        public UInt32 getOldState() {
            return this.oldState;
        }

        public UInt32 getReason() {
            return this.reason;
        }

    }
}
