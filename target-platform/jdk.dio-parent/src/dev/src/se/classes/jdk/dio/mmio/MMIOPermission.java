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
package jdk.dio.mmio;

import jdk.dio.DeviceManager;
import jdk.dio.DevicePermission;
import java.security.Permission;
import java.security.PermissionCollection;

/**
 * The {@code MMIOPermission} class defines permissions for MMIO device access.
 * <p />
 * A {@code MMIOPermission} permission has a target name and a list of actions.
 * <p />
 * The target name contains hardware addressing information. The format is the one defined for the base {@link DevicePermission} class
 * with the following addition:
 * <dl>
 * <dt><code>{channel-desc}</code></dt>
 * <dd>
 * The <code>{channel-desc}</code> string (described in {@link DevicePermission}) is
 * a memory-address (in hexadecimal format) as may be returned by a call to {@link MMIODeviceConfig#getAddress MMIODeviceConfig.getAddress}.
 * The characters in the string must all be hexadecimal digits.
 * </dd>
 * </dl>
 * The supported actions are {@code open} and {@code powermanage}. Their meaning is defined as follows:
 * <dl>
 * <dt>{@code open}</dt>
 * <dd>open and access an MMIO device functions (see {@link DeviceManager#open DeviceManager.open})</dd>
 * <dt>{@code powermanage}</dt>
 * <dd>manage the power saving mode of a device (see {@link jdk.dio.power.PowerManaged})</dd>
 * </dl>
 *
 * @see DeviceManager#open DeviceManager.open
 * @see jdk.dio.power.PowerManaged
 * @since 1.0
 */
public class MMIOPermission extends DevicePermission {

    /**
     * Constructs a new {@code MMIOPermission} with the specified target name and the implicit {@code open} action.
     *
     * @param name
     *            the target name (as defined above).
     * @throws NullPointerException
     *             if {@code name} is {@code null}.
     *
     * @see #getName getName
     */
    public MMIOPermission(String name) {
        super(name);
    }

    /**
     * Constructs a new {@code MMIOPermission} instance with the specified target name and action list.
     *
     * @param name
     *            the target name (as defined above).
     * @param actions
     *            comma-separated list of device operations: {@code open} or {@code powermanage}.
     * @throws NullPointerException
     *             if {@code name} is {@code null}.
     * @throws IllegalArgumentException
     *             if actions is {@code null}, empty or contains an action other than the
     *             specified possible actions.
     *
     * @see #getName getName
     */
    public MMIOPermission(String name, String actions) {
        super(name);
    }

    /**
     * Checks two {@code MMIOPermission} objects for equality.
     *
     * @param obj
     *            the object to test for equality with this object.
     *
     * @return {@code true} if {@code obj} is a {@code MMIOPermission} and has the same target name and actions as
     *         this {@code MMIOPermission} object.
     */
    @Override
    public boolean equals(Object obj) {
        return false;
    }

    /**
     * Returns the list of possible actions in the following order: {@code open} or {@code powermanage}. </em>
     *
     * @return comma-separated list of possible actions.
     */
    @Override
    public String getActions() {
        return null;
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return 0;
    }

    /**
     * Checks if this {@code MMIOPermission} object "implies" the specified permission.
     * <p />
     * More specifically, this method returns {@code true} if:
     * <ul>
     * <li>{@code permission} is an instance of {@code MMIOPermission}, and
     * <li>{@code permission}'s actions are a proper subset of this object's action list, and</i>
     * <li>{@code permission}'s hardware addressing information or range thereof
     * is included in this {@code MMIOPermission}'s hardware addressing information range.
     * </ul>
     *
     * @param permission
     *            the permission to check against.
     *
     * @return {@code true} if the specified permission is not {@code null} and is implied by this object, {@code false}
     *         otherwise.
     */
    @Override
    public boolean implies(Permission permission) {
        return false;
    }

    /**
     * Returns a new {@code PermissionCollection} for storing {@code MMIOPermission} objects.
     * <p>
     * {@code MMIOPermission} objects must be stored in a manner that allows them to be inserted into the collection in
     * any order, but that also enables the {@code PermissionCollection} implies method to be implemented in an
     * efficient (and consistent) manner.
     *
     * <p>
     * If {@code null} is returned, then the caller of this method is free to store permissions of this type in any
     * PermissionCollection they choose (one that uses a {@code Hashtable}, one that uses a {@code Vector}, etc).
     *
     * @return a new {@code PermissionCollection} suitable for storing {@code MMIOPermission} objects, or {@code null}
     *         if one is not defined.
     */
    @Override
    public PermissionCollection newPermissionCollection() {
        return null;
    }
}
