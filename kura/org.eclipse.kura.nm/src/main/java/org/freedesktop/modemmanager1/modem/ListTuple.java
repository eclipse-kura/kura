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

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
public class ListTuple extends Tuple {

    @Position(0)
    private String selected;
    @Position(1)
    private List<Map<String, Variant<?>>> installed;

    public ListTuple(String selected, List<Map<String, Variant<?>>> installed) {
        this.selected = selected;
        this.installed = installed;
    }

    public void setSelected(String arg) {
        this.selected = arg;
    }

    public String getSelected() {
        return this.selected;
    }

    public void setInstalled(List<Map<String, Variant<?>>> arg) {
        this.installed = arg;
    }

    public List<Map<String, Variant<?>>> getInstalled() {
        return this.installed;
    }

}
