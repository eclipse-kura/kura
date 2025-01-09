/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
package org.freedesktop.modemmanager1.modem.modem3gpp;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.ModemManager1.Modem.Modem3gpp.ProfileManager")
@DBusProperty(name = "IndexField", type = String.class, access = Access.READ)
public interface ProfileManager extends DBusInterface {

    public List<Map<String, Variant<?>>> List();

    public Map<String, Variant<?>> Set(Map<String, Variant<?>> requestedProperties);

    public void Delete(Map<String, Variant<?>> properties);

}