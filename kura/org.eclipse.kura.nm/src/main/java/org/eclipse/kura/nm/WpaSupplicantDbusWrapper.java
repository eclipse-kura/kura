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

    private static final long DEFAULT_SCAN_TIMEOUT_SECONDS = 30;

    private final DBusConnection dbusConnection;
    private final Wpa_supplicant1 wpaSupplicant;

    public WpaSupplicantDbusWrapper(DBusConnection dbusConnection) throws DBusException {
        this.dbusConnection = dbusConnection;
        this.wpaSupplicant = this.dbusConnection.getRemoteObject(WPA_SUPPLICANT_BUS_NAME, WPA_SUPPLICANT_BUS_PATH,
                Wpa_supplicant1.class);
    }

    public void syncScan(String interfaceName) throws DBusException {
        syncScan(interfaceName, DEFAULT_SCAN_TIMEOUT_SECONDS);
    }

    public void syncScan(String interfaceName, long scanTimeoutSeconds) throws DBusException {
        DBusPath interfacePath = this.wpaSupplicant.GetInterface(interfaceName);

        WPAScanLock scanLock = new WPAScanLock(this.dbusConnection, interfacePath.getPath());

        triggerScan(interfacePath);

        scanLock.waitForSignal(scanTimeoutSeconds);
    }

    public DBusPath asyncScan(String interfaceName) throws DBusException {
        DBusPath interfacePath = this.wpaSupplicant.GetInterface(interfaceName);

        triggerScan(interfacePath);

        return interfacePath;
    }

    private void triggerScan(DBusPath interfaceObjectPath) throws DBusException {
        Interface interfaceObject = this.dbusConnection.getRemoteObject(WPA_SUPPLICANT_BUS_NAME,
                interfaceObjectPath.getPath(), Interface.class);

        Map<String, Variant<?>> options = new HashMap<>();
        options.put("Type", new Variant<>("active"));
        options.put("AllowRoam", new Variant<>(false));

        interfaceObject.Scan(options);
    }

}
