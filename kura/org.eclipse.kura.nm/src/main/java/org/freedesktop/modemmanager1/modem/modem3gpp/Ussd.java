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
package org.freedesktop.modemmanager1.modem.modem3gpp;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt32;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.ModemManager1.Modem.Modem3gpp.Ussd")
@DBusProperty(name = "State", type = UInt32.class, access = Access.READ)
@DBusProperty(name = "NetworkNotification", type = String.class, access = Access.READ)
@DBusProperty(name = "NetworkRequest", type = String.class, access = Access.READ)
public interface Ussd extends DBusInterface {

    public String Initiate(String command);

    public String Respond(String response);

    public void Cancel();

}
