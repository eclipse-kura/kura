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

package jdk.dio.pwm;

import jdk.dio.DeviceManager;
import jdk.dio.DevicePermission;
import java.security.Permission;
import java.security.PermissionCollection;

/**
 * The {@code PWMPermission} class defines permissions for PWM channel access.
 * <p />
 * A {@code PWMPermission} permission has a target name and a list of actions.
 * <p />
 * The target name contains hardware addressing information. The format is the one defined for the base {@link DevicePermission} class
 * with the following addition:
 * <dl>
 * <dt><code>{channel-desc}</code></dt>
 * <dd>
 * The <code>{channel-desc}</code> string (described in {@link DevicePermission}) is
 * the decimal string representation of a channel number as may be returned by a call to
 * {@link PWMChannelConfig#getChannelNumber PWMChannelConfig.getChannelNumber}. The characters in the string must all be decimal digits.
 * </dd>
 * </dl>
 * The supported actions are {@code open} and {@code powermanage}. Their meaning is defined as follows:
 * <dl>
 * <dt>{@code open}</dt>
 * <dd>open and access an PWM channel functions (see {@link DeviceManager#open DeviceManager.open})</dd>
 * <dt>{@code powermanage}</dt>
 * <dd>manage the power saving mode of a device (see {@link jdk.dio.power.PowerManaged})</dd>
 * </dl>
 *
 * @see DeviceManager#open DeviceManager.open
 * @see jdk.dio.power.PowerManaged
 * @since 1.0
 */
@SuppressWarnings("serial")
public class PWMPermission extends DevicePermission {

    /**
     * Constructs a new {@code PWMPermission} with the specified target name and the implicit {@code open} action.
     *
     * @param name
     *            the target name (as defined above).
     * @throws NullPointerException
     *             if {@code name} is {@code null}.
     *
     * @see #getName getName
     */
    public PWMPermission(String name) {
        super(name);
    }

    /**
     * Constructs a new {@code PWMPermission} instance with the specified target name and action list.
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
    public PWMPermission(String name, String actions) {
        super(name, actions);
    }

}

