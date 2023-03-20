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
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.ModemManager1.Modem.Signal")
@DBusProperty(name = "Rate", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Cdma", type = Signal.PropertyCdmaType.class, access = Access.READ)
@DBusProperty(name = "Evdo", type = Signal.PropertyEvdoType.class, access = Access.READ)
@DBusProperty(name = "Gsm", type = Signal.PropertyGsmType.class, access = Access.READ)
@DBusProperty(name = "Umts", type = Signal.PropertyUmtsType.class, access = Access.READ)
@DBusProperty(name = "Lte", type = Signal.PropertyLteType.class, access = Access.READ)
public interface Signal extends DBusInterface {

    public void Setup(UInt32 rate);

    public static interface PropertyCdmaType extends TypeRef<Map<String, Variant>> {

    }

    public static interface PropertyEvdoType extends TypeRef<Map<String, Variant>> {

    }

    public static interface PropertyGsmType extends TypeRef<Map<String, Variant>> {

    }

    public static interface PropertyUmtsType extends TypeRef<Map<String, Variant>> {

    }

    public static interface PropertyLteType extends TypeRef<Map<String, Variant>> {

    }
}
