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
@DBusInterfaceName("org.freedesktop.ModemManager1.Modem.Location")
@DBusProperty(name = "Capabilities", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "SupportedAssistanceData", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Enabled", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "SignalsLocation", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "Location", type = Location.PropertyLocationType.class, access = Access.READ)
@DBusProperty(name = "SuplServer", type = String.class, access = Access.READ)
@DBusProperty(name = "AssistanceDataServers", type = Location.PropertyAssistanceDataServersType.class, access = Access.READ)
@DBusProperty(name = "GpsRefreshRate", type = UInt32.class, access = Access.READ)
public interface Location extends DBusInterface {

    public void Setup(UInt32 sources, boolean signalLocation);

    public Map<UInt32, Variant<?>> GetLocation();

    public void SetSuplServer(String supl);

    public void InjectAssistanceData(List<Byte> data);

    public void SetGpsRefreshRate(UInt32 rate);

    public static interface PropertyLocationType extends TypeRef<Map<UInt32, Variant>> {

    }

    public static interface PropertyAssistanceDataServersType extends TypeRef<List<String>> {

    }
}
