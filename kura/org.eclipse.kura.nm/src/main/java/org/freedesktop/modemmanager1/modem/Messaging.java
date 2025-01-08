/*******************************************************************************
 * Copyright (c) 2023, 2025 Eurotech and/or its affiliates and others
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

    List<DBusPath> List();

    void Delete(DBusPath path);

    DBusPath Create(Map<String, Variant<?>> properties);

    public static class Added extends DBusSignal {

        private final DBusPath path;
        private final boolean received;

        public Added(String _Spath, DBusPath _path, boolean _received) throws DBusException {
            super(_Spath, _path, _received);
            this.path = _path;
            this.received = _received;
        }

        public DBusPath getDbusPath() {
            return path;
        }

        public boolean getReceived() {
            return received;
        }

    }

    public static class Deleted extends DBusSignal {

        private final DBusPath path;

        public Deleted(String _Spath, DBusPath _path) throws DBusException {
            super(_Spath, _path);
            this.path = _path;
        }

        public DBusPath getDbusPath() {
            return path;
        }

    }

    public static interface PropertyMessagesType extends TypeRef<List<DBusPath>> {

    }

    public static interface PropertySupportedStoragesType extends TypeRef<List<UInt32>> {

    }

}
