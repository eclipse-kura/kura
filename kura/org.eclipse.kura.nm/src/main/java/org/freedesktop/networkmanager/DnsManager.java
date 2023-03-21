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
package org.freedesktop.networkmanager;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.NetworkManager.DnsManager")
@DBusProperty(name = "Mode", type = String.class, access = Access.READ)
@DBusProperty(name = "RcManager", type = String.class, access = Access.READ)
@DBusProperty(name = "Configuration", type = DnsManager.PropertyConfigurationType.class, access = Access.READ)
public interface DnsManager extends DBusInterface {

    public static interface PropertyConfigurationType extends TypeRef<List<Map<String, Variant>>> {

    }
}
