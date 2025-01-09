/*******************************************************************************
 * Copyright (c) 2023, 2025 Eurotech and/or its affiliates and others
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
@DBusInterfaceName("org.freedesktop.ModemManager1.Sim")
@DBusProperty(name = "Active", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "SimIdentifier", type = String.class, access = Access.READ)
@DBusProperty(name = "Imsi", type = String.class, access = Access.READ)
@DBusProperty(name = "Eid", type = String.class, access = Access.READ)
@DBusProperty(name = "OperatorIdentifier", type = String.class, access = Access.READ)
@DBusProperty(name = "OperatorName", type = String.class, access = Access.READ)
@DBusProperty(name = "EmergencyNumbers", type = Sim.PropertyEmergencyNumbersType.class, access = Access.READ)
@DBusProperty(name = "PreferredNetworks", type = Sim.PropertyPreferredNetworksType.class, access = Access.READ)
@DBusProperty(name = "Gid1", type = Sim.PropertyGid1Type.class, access = Access.READ)
@DBusProperty(name = "Gid2", type = Sim.PropertyGid2Type.class, access = Access.READ)
@DBusProperty(name = "SimType", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "EsimStatus", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "Removability", type = UInt32.class, access = Access.READ)
public interface Sim extends DBusInterface {

    public void SendPin(String pin);

    public void SendPuk(String puk, String pin);

    public void EnablePin(String pin, boolean enabled);

    public void ChangePin(String oldPin, String newPin);

    public void SetPreferredNetworks(List<SetPreferredNetworksStruct> preferredNetworks);

    public static interface PropertyEmergencyNumbersType extends TypeRef<List<String>> {

    }

    public static interface PropertyPreferredNetworksType extends TypeRef<List<PropertyPreferredNetworksStruct>> {

    }

    public static interface PropertyGid1Type extends TypeRef<List<Byte>> {

    }

    public static interface PropertyGid2Type extends TypeRef<List<Byte>> {

    }
}
