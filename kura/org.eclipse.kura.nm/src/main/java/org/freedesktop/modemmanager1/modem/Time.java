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

import java.util.Map;

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
@DBusInterfaceName("org.freedesktop.ModemManager1.Modem.Time")
@DBusProperty(name = "NetworkTimezone", type = Time.PropertyNetworkTimezoneType.class, access = Access.READ)
public interface Time extends DBusInterface {

    public String GetNetworkTime();

    public static interface PropertyNetworkTimezoneType extends TypeRef<Map<String, Variant>> {

    }

    public static class NetworkTimeChanged extends DBusSignal {

        private final String time;

        public NetworkTimeChanged(String _path, String _time) throws DBusException {
            super(_path, _time);
            this.time = _time;
        }

        public String getTime() {
            return this.time;
        }

    }
}
