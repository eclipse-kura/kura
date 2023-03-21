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

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.freedesktop.NetworkManager.SecretAgent")
public interface SecretAgent extends DBusInterface {

    public Map<String, Map<String, Variant<?>>> GetSecrets(Map<String, Map<String, Variant<?>>> connection,
            DBusPath connectionPath, String settingName, List<String> hints, UInt32 flags);

    public void CancelGetSecrets(DBusPath connectionPath, String settingName);

    public void SaveSecrets(Map<String, Map<String, Variant<?>>> connection, DBusPath connectionPath);

    public void DeleteSecrets(Map<String, Map<String, Variant<?>>> connection, DBusPath connectionPath);

}
