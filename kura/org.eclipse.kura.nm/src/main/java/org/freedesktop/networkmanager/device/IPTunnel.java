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
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.NetworkManager.Device.IPTunnel")
@DBusProperty(name = "Mode", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Parent", type = DBusPath.class, access = Access.READ)
@DBusProperty(name = "Local", type = String.class, access = Access.READ)
@DBusProperty(name = "Remote", type = String.class, access = Access.READ)
@DBusProperty(name = "Ttl", type = Byte.class, access = Access.READ)
@DBusProperty(name = "Tos", type = Byte.class, access = Access.READ)
@DBusProperty(name = "PathMtuDiscovery", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "InputKey", type = String.class, access = Access.READ)
@DBusProperty(name = "OutputKey", type = String.class, access = Access.READ)
@DBusProperty(name = "EncapsulationLimit", type = Byte.class, access = Access.READ)
@DBusProperty(name = "FlowLabel", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Flags", type = UInt32.class, access = Access.READ)
public interface IPTunnel extends DBusInterface {

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
