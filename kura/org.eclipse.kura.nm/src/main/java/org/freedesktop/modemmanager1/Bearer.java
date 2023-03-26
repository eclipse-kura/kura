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
package org.freedesktop.modemmanager1;

import java.util.Map;

import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.ModemManager1.Bearer")
@DBusProperty(name = "Interface", type = String.class, access = Access.READ)
@DBusProperty(name = "Connected", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "Suspended", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "Ip4Config", type = Bearer.PropertyIp4ConfigType.class, access = Access.READ)
@DBusProperty(name = "Ip6Config", type = Bearer.PropertyIp6ConfigType.class, access = Access.READ)
@DBusProperty(name = "Stats", type = Bearer.PropertyStatsType.class, access = Access.READ)
@DBusProperty(name = "IpTimeout", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "BearerType", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Properties", type = Bearer.PropertyPropertiesType.class, access = Access.READ)
public interface Bearer extends DBusInterface {

    public void Connect();

    public void Disconnect();

    public static interface PropertyIp4ConfigType extends TypeRef<Map<String, Variant>> {

    }

    public static interface PropertyIp6ConfigType extends TypeRef<Map<String, Variant>> {

    }

    public static interface PropertyStatsType extends TypeRef<Map<String, Variant>> {

    }

    public static interface PropertyPropertiesType extends TypeRef<Map<String, Variant>> {

    }
}
