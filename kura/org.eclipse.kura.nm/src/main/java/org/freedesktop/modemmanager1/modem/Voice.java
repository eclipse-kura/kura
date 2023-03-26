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

        private final DBusPath dbusPath;

        public CallAdded(String _path, DBusPath _dbusPath) throws DBusException {
            super(_path, _dbusPath);
            this.dbusPath = _dbusPath;
        }

        public DBusPath getDbusPath() {
            return this.dbusPath;
        }

    }

    public static class CallDeleted extends DBusSignal {

        private final DBusPath dbusPath;

        public CallDeleted(String _path, DBusPath _dbusPath) throws DBusException {
            super(_path, _dbusPath);
            this.dbusPath = _dbusPath;
        }

        public DBusPath getDbusPath() {
            return this.dbusPath;
        }

    }

    public static interface PropertyCallsType extends TypeRef<List<DBusPath>> {

    }
}
