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

import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt32;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.NetworkManager.WifiP2PPeer")
@DBusProperty(name = "Name", type = String.class, access = Access.READ)
@DBusProperty(name = "Flags", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Manufacturer", type = String.class, access = Access.READ)
@DBusProperty(name = "Model", type = String.class, access = Access.READ)
@DBusProperty(name = "ModelNumber", type = String.class, access = Access.READ)
@DBusProperty(name = "Serial", type = String.class, access = Access.READ)
@DBusProperty(name = "WfdIEs", type = WifiP2PPeer.PropertyWfdIEsType.class, access = Access.READ)
@DBusProperty(name = "HwAddress", type = String.class, access = Access.READ)
@DBusProperty(name = "Strength", type = Byte.class, access = Access.READ)
@DBusProperty(name = "LastSeen", type = Integer.class, access = Access.READ)
public interface WifiP2PPeer extends DBusInterface {

    public static interface PropertyWfdIEsType extends TypeRef<List<Byte>> {

    }
}
