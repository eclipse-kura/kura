/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.dio.generic;

/**
 * The class {@code GenericDeviceControl} encapsulates a generic device's configuration
 * and access (I/O) controls. A control can be set or gotten using the
 * {@link GenericDevice#setControl GenericDevice.setControl} and {@link GenericDevice#getControl
 * GenericDevice.getControl} methods. Controls can be used to configure a generic device
 * a well as performing basic input/output operations. The list of controls supported by a
 * device is device-specific.
 *
 * @param <T>
 *            the type of the control's value.
 * @see GenericDevice#getControl GenericDevice.getControl
 * @see GenericDevice#setControl GenericDevice.setControl
 * @since 1.0
 */
public class GenericDeviceControl<T> {
    private int id = -1;
    private Class<T> type = null;

    /**
     * Creates a new {@code GenericDeviceControl} with the specified ID and type.
     *
     * @param id
     *            the ID of the control.
     * @param type
     *            the type of the control's value.
     */
    public GenericDeviceControl(int id, Class<T> type) {
        this.id = id;
        this.type = type;
    }

    /**
     * Gets the ID of this control.
     *
     * @return the ID of this control.
     */
    public int getID() {
        return id;
    }

    /**
     * Gets the type of this control's value.
     *
     * @return the type of this control's value.
     */
    public Class<T> getType() {
        return type;
    }
}
