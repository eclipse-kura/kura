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
package org.freedesktop;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class AddAndActivateConnectionTuple extends Tuple {

    @Position(0)
    private DBusPath path;
    @Position(1)
    private DBusPath activeConnection;

    public AddAndActivateConnectionTuple(DBusPath path, DBusPath activeConnection) {
        this.path = path;
        this.activeConnection = activeConnection;
    }

    public void setPath(DBusPath arg) {
        this.path = arg;
    }

    public DBusPath getPath() {
        return this.path;
    }

    public void setActiveConnection(DBusPath arg) {
        this.activeConnection = arg;
    }

    public DBusPath getActiveConnection() {
        return this.activeConnection;
    }

}
