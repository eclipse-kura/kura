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

import java.util.Map;

import org.freedesktop.dbus.DBusPath;
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
@DBusInterfaceName("org.freedesktop.NetworkManager.Device.Macsec")
@DBusProperty(name = "Parent", type = DBusPath.class, access = Access.READ)
@DBusProperty(name = "Sci", type = UInt64.class, access = Access.READ)
@DBusProperty(name = "IcvLength", type = Byte.class, access = Access.READ)
@DBusProperty(name = "CipherSuite", type = UInt64.class, access = Access.READ)
@DBusProperty(name = "Window", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "EncodingSa", type = Byte.class, access = Access.READ)
@DBusProperty(name = "Validation", type = String.class, access = Access.READ)
@DBusProperty(name = "Encrypt", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "Protect", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "IncludeSci", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "Es", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "Scb", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "ReplayProtect", type = Boolean.class, access = Access.READ)
public interface Macsec extends DBusInterface {

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
}
