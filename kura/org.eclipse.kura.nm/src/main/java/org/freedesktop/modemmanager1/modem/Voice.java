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
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.ModemManager1.Modem.Voice")
@DBusProperty(name = "Calls", type = Voice.PropertyCallsType.class, access = Access.READ)
@DBusProperty(name = "EmergencyOnly", type = Boolean.class, access = Access.READ)
public interface Voice extends DBusInterface {

    List<DBusPath> ListCalls();

    void DeleteCall(DBusPath path);

    DBusPath CreateCall(Map<String, Variant<?>> properties);

    void HoldAndAccept();

    void HangupAndAccept();

    void HangupAll();

    void Transfer();

    void CallWaitingSetup(boolean enable);

    boolean CallWaitingQuery();

    public static class CallAdded extends DBusSignal {

        private final DBusPath path;

        public CallAdded(String _Spath, DBusPath _path) throws DBusException {
            super(_Spath, _path);
            this.path = _path;
        }

        public DBusPath getDbusPath() {
            return path;
        }

    }

    public static class CallDeleted extends DBusSignal {

        private final DBusPath path;

        public CallDeleted(String _Spath, DBusPath _path) throws DBusException {
            super(_Spath, _path);
            this.path = _path;
        }

        public DBusPath getDbusPath() {
            return path;
        }

    }

    public static interface PropertyCallsType extends TypeRef<List<DBusPath>> {

    }

}
