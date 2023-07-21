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
package org.eclipse.kura.nm;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.nm.signal.handlers.WPAScanLock;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.Variant;

import fi.w1.Wpa_supplicant1;
import fi.w1.wpa_supplicant1.Interface;

public class WpaSupplicantDbusWrapper {

    private static final String WPA_SUPPLICANT_BUS_NAME = "fi.w1.wpa_supplicant1";
    private static final String WPA_SUPPLICANT_BUS_PATH = "/fi/w1/wpa_supplicant1";

    private final DBusConnection dbusConnection;
    private Wpa_supplicant1 wpaSupplicant;

    public WpaSupplicantDbusWrapper(DBusConnection dbusConnection) throws DBusException {
        this.dbusConnection = dbusConnection;
        this.wpaSupplicant = this.dbusConnection.getRemoteObject(WPA_SUPPLICANT_BUS_NAME, WPA_SUPPLICANT_BUS_PATH,
                Wpa_supplicant1.class);
    }

    public void triggerScan(String interfaceName) throws DBusException {
        DBusPath interfacePath = this.wpaSupplicant.GetInterface(interfaceName);

        Interface interfaceObject = this.dbusConnection.getRemoteObject(WPA_SUPPLICANT_BUS_NAME,
                interfacePath.getPath(), Interface.class);

        Map<String, Variant<?>> options = new HashMap<>();
        options.put("Type", new Variant<>("active"));
        options.put("AllowRoam", new Variant<>(false));

        WPAScanLock scanLock = new WPAScanLock(this.dbusConnection, interfacePath.getPath());

        interfaceObject.Scan(options);
        scanLock.waitForSignal();
    }

}
