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
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.NetworkManager.Settings")
@DBusProperty(name = "Connections", type = Settings.PropertyConnectionsType.class, access = Access.READ)
@DBusProperty(name = "Hostname", type = String.class, access = Access.READ)
@DBusProperty(name = "CanModify", type = Boolean.class, access = Access.READ)
public interface Settings extends DBusInterface {

    public List<DBusPath> ListConnections();

    public DBusPath GetConnectionByUuid(String uuid);

    public DBusPath AddConnection(Map<String, Map<String, Variant<?>>> connection);

    public DBusPath AddConnectionUnsaved(Map<String, Map<String, Variant<?>>> connection);

    public AddConnection2Tuple AddConnection2(Map<String, Map<String, Variant<?>>> settings, UInt32 flags,
            Map<String, Variant<?>> args);

    public LoadConnectionsTuple LoadConnections(List<String> filenames);

    public boolean ReloadConnections();

    public void SaveHostname(String hostname);

    public static interface PropertyConnectionsType extends TypeRef<List<DBusPath>> {

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

    public static class NewConnection extends DBusSignal {

        private final DBusPath connection;

        public NewConnection(String _path, DBusPath _connection) throws DBusException {
            super(_path, _connection);
            this.connection = _connection;
        }

        public DBusPath getConnection() {
            return this.connection;
        }

    }

    public static class ConnectionRemoved extends DBusSignal {

        private final DBusPath connection;

        public ConnectionRemoved(String _path, DBusPath _connection) throws DBusException {
            super(_path, _connection);
            this.connection = _connection;
        }

        public DBusPath getConnection() {
            return this.connection;
        }

    }
}
