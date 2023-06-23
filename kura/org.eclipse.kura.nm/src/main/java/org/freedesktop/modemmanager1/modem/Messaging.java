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

        private final DBusPath dbusPath;
        private final boolean received;

        public Added(String _path, DBusPath _dbusPath, boolean _received) throws DBusException {
            super(_path, _dbusPath, _received);
            this.dbusPath = _dbusPath;
            this.received = _received;
        }

        public DBusPath getDbusPath() {
            return this.dbusPath;
        }

        public boolean getReceived() {
            return this.received;
        }

    }

    public static class Deleted extends DBusSignal {

        private final DBusPath dbusPath;

        public Deleted(String _path, DBusPath _dbusPath) throws DBusException {
            super(_path, _dbusPath);
            this.dbusPath = _dbusPath;
        }

        public DBusPath getDbusPath() {
            return this.dbusPath;
        }

    }

    public static interface PropertyMessagesType extends TypeRef<List<DBusPath>> {

    }

    public static interface PropertySupportedStoragesType extends TypeRef<List<UInt32>> {

    }
}
