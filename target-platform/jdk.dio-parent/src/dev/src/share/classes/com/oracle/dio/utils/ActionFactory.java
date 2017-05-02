/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.dio.utils;

import java.security.Permission;
import jdk.dio.DevicePermission;
import jdk.dio.DeviceMgmtPermission;
import jdk.dio.atcmd.ATPermission;
import jdk.dio.gpio.GPIOPinPermission;
import jdk.dio.gpio.GPIOPortPermission;

/**
 * Utility class for device permission class
 */
public class ActionFactory {
    /**
     * Return true if all actions are applied for permission
     *
     * @param actions for validation
     * @param permission to verify which action implied
     *
     * @return true for valid
     */
    public static boolean actionImplies(String actions, Permission permission) {
        int index = actions.indexOf(",");
        if (index == -1) {
            return isValidAction(actions, permission) && (permission.getActions().indexOf(actions) != -1);
        } else {
            return actionImplies(actions.substring(0, index), permission) &&
                   actionImplies(actions.substring(index+1), permission);
        }
    }

    /**
     * Return true for permission specified actions
     *
     * @param action for validation
     * @param permission to verify which action implied
     *
     * @return true for valid
     */
    private static boolean isValidAction(String action, Permission permission) {
        if (permission instanceof DevicePermission) {
            if (DevicePermission.OPEN.equals(action)) return true;
            if (DevicePermission.POWER_MANAGE.equals(action)) return true;
        }
        if (permission instanceof DeviceMgmtPermission) {
            if (DeviceMgmtPermission.REGISTER.equals(action)) return true;
            if (DeviceMgmtPermission.UNREGISTER.equals(action)) return true;
            if (DeviceMgmtPermission.OPEN.equals(action)) return true;
        }
        if (permission instanceof ATPermission)
            if (ATPermission.DATA.equals(action)) return true;
        if (permission instanceof GPIOPinPermission)
            if (GPIOPinPermission.SET_DIRECTION.equals(action)) return true;
        if (permission instanceof GPIOPortPermission)
            if (GPIOPortPermission.SET_DIRECTION.equals(action)) return true;

        return false;
    }

    /**
     * Returns action list in spec required order
     * <p>
     * It is assumed that actions have the following priority
     * <ul>
     *     <li> open </li>
     *     <li> powermanage </li>
     *     <li> other peripheral specific actions </li>
     * </ul>
     *
     * @param actions unordered and {@code verified} list
     * @param permission to verify which action implied
     *
     * @return ordered list
     */
    public static String verifyAndOrderActions(String actions, Permission permission) {
        int idx = 0;
        int prevIdx = 0;
        while (-1 != (idx = actions.indexOf(',', prevIdx))) {
            String s1 = actions.substring(prevIdx, idx);
            // check for duplicate and validity
            if (!isValidAction(s1, permission) || -1 != actions.indexOf(s1, idx)) {
                throw new IllegalArgumentException(actions);
            }
            prevIdx = idx + 1;
        }
        if (!isValidAction(actions.substring(prevIdx), permission) ) {
                throw new IllegalArgumentException(actions);
        }

        StringBuilder sb = new StringBuilder(30);
        if (permission instanceof DeviceMgmtPermission) {
            if (-1 != actions.indexOf(DeviceMgmtPermission.REGISTER)) {
                sb.append(DeviceMgmtPermission.REGISTER);
                idx = 1;
            }
            if (-1 != actions.indexOf(DeviceMgmtPermission.UNREGISTER)) {
                if (idx > 0 ) {
                    sb.append(',');
                }
                sb.append(DeviceMgmtPermission.UNREGISTER);
                idx = 1;
            }

            if (-1 != actions.indexOf(DeviceMgmtPermission.OPEN)) {
                if (idx > 0 ) {
                    sb.append(',');
                }
                sb.append(DeviceMgmtPermission.OPEN);
                idx = 1;
            }
        }
        if (permission instanceof DevicePermission) {
            if (-1 != actions.indexOf(DevicePermission.OPEN)) {
                sb.append(DevicePermission.OPEN);
                idx = 1;
            }
            if (-1 != actions.indexOf(DevicePermission.POWER_MANAGE)) {
                if (idx > 0 ) {
                    sb.append(',');
                }
                sb.append(DevicePermission.POWER_MANAGE);
                idx = 1;
            }
        }
        if ((permission instanceof ATPermission) && -1 != actions.indexOf(ATPermission.DATA)) {
            if (idx > 0 ) {
                sb.append(',');
            }
            sb.append(ATPermission.DATA);
            idx = 1;
        }
        if ((permission instanceof GPIOPinPermission) && -1 != actions.indexOf(GPIOPinPermission.SET_DIRECTION)) {
            if (idx > 0 ) {
                sb.append(',');
            }
            sb.append(GPIOPinPermission.SET_DIRECTION);
            idx = 1;
        }
        if ((permission instanceof GPIOPortPermission) && -1 != actions.indexOf(GPIOPortPermission.SET_DIRECTION)) {
            if (idx > 0 ) {
                sb.append(',');
            }
            sb.append(GPIOPortPermission.SET_DIRECTION);
            idx = 1;
        }
        return sb.toString();
    }
}