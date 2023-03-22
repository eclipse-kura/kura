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
package org.freedesktop.networkmanager.settings;

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
@DBusInterfaceName("org.freedesktop.NetworkManager.Settings.Connection")
@DBusProperty(name = "Unsaved", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "Flags", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Filename", type = String.class, access = Access.READ)
public interface Connection extends DBusInterface {

    public void Update(Map<String, Map<String, Variant<?>>> properties);

    public void UpdateUnsaved(Map<String, Map<String, Variant<?>>> properties);

    public void Delete();

    public Map<String, Map<String, Variant<?>>> GetSettings();

    public Map<String, Map<String, Variant<?>>> GetSecrets(String settingName);

    public void ClearSecrets();

    public void Save();

    public Map<String, Variant<?>> Update2(Map<String, Map<String, Variant<?>>> settings, UInt32 flags,
            Map<String, Variant<?>> args);

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
