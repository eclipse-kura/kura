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

import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
public class AddConnection2Tuple extends Tuple {

    @Position(0)
    private DBusPath path;
    @Position(1)
    private Map<String, Variant<?>> result;

    public AddConnection2Tuple(DBusPath path, Map<String, Variant<?>> result) {
        this.path = path;
        this.result = result;
    }

    public void setPath(DBusPath arg) {
        this.path = arg;
    }

    public DBusPath getPath() {
        return this.path;
    }

    public void setResult(Map<String, Variant<?>> arg) {
        this.result = arg;
    }

    public Map<String, Variant<?>> getResult() {
        return this.result;
    }

}
