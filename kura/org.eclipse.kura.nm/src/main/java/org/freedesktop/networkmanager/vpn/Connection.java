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
package org.freedesktop.networkmanager.vpn;

import java.util.Map;

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
@DBusInterfaceName("org.freedesktop.NetworkManager.VPN.Connection")
@DBusProperty(name = "VpnState", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Banner", type = String.class, access = Access.READ)
public interface Connection extends DBusInterface {

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

    public static class VpnStateChanged extends DBusSignal {

        private final UInt32 state;
        private final UInt32 reason;

        public VpnStateChanged(String _path, UInt32 _state, UInt32 _reason) throws DBusException {
            super(_path, _state, _reason);
            this.state = _state;
            this.reason = _reason;
        }

        public UInt32 getState() {
            return this.state;
        }

        public UInt32 getReason() {
            return this.reason;
        }

    }
}
