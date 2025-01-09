/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class PropertyConnectionErrorStruct extends Struct {

    @Position(0)
    private final String member0;
    @Position(1)
    private final String member1;

    public PropertyConnectionErrorStruct(String member0, String member1) {
        this.member0 = member0;
        this.member1 = member1;
    }

    public String getMember0() {
        return this.member0;
    }

    public String getMember1() {
        return this.member1;
    }

}