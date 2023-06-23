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

import java.util.List;

import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.ModemManager1.Sim")
@DBusProperty(name = "SimIdentifier", type = String.class, access = Access.READ)
@DBusProperty(name = "Imsi", type = String.class, access = Access.READ)
@DBusProperty(name = "OperatorIdentifier", type = String.class, access = Access.READ)
@DBusProperty(name = "OperatorName", type = String.class, access = Access.READ)
@DBusProperty(name = "EmergencyNumbers", type = Sim.PropertyEmergencyNumbersType.class, access = Access.READ)
public interface Sim extends DBusInterface {

    public void SendPin(String pin);

    public void SendPuk(String puk, String pin);

    public void EnablePin(String pin, boolean enabled);

    public void ChangePin(String oldPin, String newPin);

    public static interface PropertyEmergencyNumbersType extends TypeRef<List<String>> {

    }
}
