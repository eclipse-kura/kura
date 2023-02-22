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

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt32;

/**
 * Auto-generated class.
 */
public class PropertySignalQualityStruct extends Struct {

    @Position(0)
    private final UInt32 member0;
    @Position(1)
    private final boolean member1;

    public PropertySignalQualityStruct(UInt32 member0, boolean member1) {
        this.member0 = member0;
        this.member1 = member1;
    }

    public UInt32 getMember0() {
        return member0;
    }

    public boolean getMember1() {
        return member1;
    }

}
