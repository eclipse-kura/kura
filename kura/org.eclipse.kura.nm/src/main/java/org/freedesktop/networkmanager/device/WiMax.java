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
package org.freedesktop.networkmanager.device;

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
@DBusInterfaceName("org.freedesktop.NetworkManager.Device.WiMax")
@DBusProperty(name = "Nsps", type = WiMax.PropertyNspsType.class, access = Access.READ)
@DBusProperty(name = "HwAddress", type = String.class, access = Access.READ)
@DBusProperty(name = "CenterFrequency", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Rssi", type = Integer.class, access = Access.READ)
@DBusProperty(name = "Cinr", type = Integer.class, access = Access.READ)
@DBusProperty(name = "TxPower", type = Integer.class, access = Access.READ)
@DBusProperty(name = "Bsid", type = String.class, access = Access.READ)
@DBusProperty(name = "ActiveNsp", type = DBusPath.class, access = Access.READ)
public interface WiMax extends DBusInterface {

    public List<DBusPath> GetNspList();

    public static interface PropertyNspsType extends TypeRef<List<DBusPath>> {

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

    public static class NspAdded extends DBusSignal {

        private final DBusPath nsp;

        public NspAdded(String _path, DBusPath _nsp) throws DBusException {
            super(_path, _nsp);
            this.nsp = _nsp;
        }

        public DBusPath getNsp() {
            return this.nsp;
        }

    }

    public static class NspRemoved extends DBusSignal {

        private final DBusPath nsp;

        public NspRemoved(String _path, DBusPath _nsp) throws DBusException {
            super(_path, _nsp);
            this.nsp = _nsp;
        }

        public DBusPath getNsp() {
            return this.nsp;
        }

    }
}
