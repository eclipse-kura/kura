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

package jdk.dio;

import java.security.Permission;
import java.security.PermissionCollection;
import com.oracle.dio.utils.ActionFactory;
import java.util.Vector;
import com.oracle.dio.utils.ExceptionMessage;

/**
 * The {@code DeviceMgmtPermission} class defines permissions for registering and unregistering devices as
 * well as opening devices using their registered configurations.
 * <p />
 * Device management permissions have a target name and actions.
 * <p />
 * The target name is a combination of a device name and of a device ID or range of device IDs.
 * It takes the following form:
 * <blockquote>
 * <code>{device-name-spec} [ ":"{device-id-spec} ]</code>
 * </blockquote>
 * <dl>
 * <dt><code>{device-name-spec}</code></dt>
 * <dd>
 * The <code>{device-name-spec}</code> string takes the following form:
 * <blockquote>
 * <code>{device-name} | "*" | ""</code>
 * </blockquote>
 * The <code>{device-name}</code>string is a device name as may be returned by a call to {@link DeviceDescriptor#getName() DeviceDescriptor.getName}.
 * <br />
 * A <code>{device-name-spec}</code> specification consisting of the asterisk ("*") matches all device names.
 * A <code>{device-name-spec}</code> specification consisting of the empty string ("") designates an undefined device name
 * that may only be matched by an empty string or an asterisk.
 * </dd>
 * <dt><code>{device-id-spec}</code></dt>
 * <dd>
 * The <code>{device-id-spec}</code> string takes the following form:
 * <blockquote>
 * <code>{device-id} | "-"{device-id} | {device-id}"-"[{device-id}] | "*"</code>
 * </blockquote>
 * The <code>{device-id}</code> string is a device ID as may be returned by a call to {@link DeviceDescriptor#getID() DeviceDescriptor.getID}.
 * The characters in the string must all be decimal digits.
 * <br />
 * A <code>{device-id-spec}</code> specification of the form "N-" (where N is a device ID) signifies all device IDs
 * numbered N and above, while a specification of the form "-N" indicates all device IDs numbered N and below.
 * A single asterisk in the place of the <code>{device-id-spec}</code> field matches all device IDs.
 * <p />
 * The target name {@code "*:*"} matches all device names and all device IDs as is the target name {@code "*"}.
 * </dd>
 * </dt>
 * The actions to be granted are passed to the constructor in a string containing a list of one or more comma-separated
 * keywords. The possible keywords are {@code register} and {@code unregister}. Their
 * meaning is defined as follows:
 * <dl>
 * <dt>{@code open}</dt>
 * <dd>open a device using its device ID or name (see {@link DeviceManager#open(int) DeviceManager.open(id, ...)}
 * and {@link DeviceManager#open(java.lang.String, java.lang.Class, java.lang.String[]) DeviceManager.open(name, ...)} methods)</dd>
 * <dt>{@code register}</dt>
 * <dd>register a new device</dd>
 * <dt>{@code unregister}</dt>
 * <dd>unregister a device</dd>
 * </dl>
 *
 * @see DeviceManager#open DeviceManager.open
 * @see DeviceManager#register DeviceManager.register
 * @see DeviceManager#unregister DeviceManager.unregister
 * @since 1.0
 */
public class DeviceMgmtPermission  extends Permission {

    /**
     * The {@code register} action.
     */
    public static final String REGISTER = "register";

    /**
     * The {@code unregister} action.
     */
    public static final String UNREGISTER = "unregister";

    /**
     * The {@code open} action.
     */
    public static final String OPEN = "open";

    /** Comma-delimited ordered action list */
    private String myActions;

    /**
     * Constructs a new {@code DeviceMgmtPermission} instance with the specified target name and action list.
     *
     * @param name
     *            the target name (as defined above).
     * @param actions
     *            comma-separated list of device management operations: {@code register}
     *            {@code unregister} or {@code open}.
     * @throws NullPointerException
     *             if {@code name} is {@code null}.
     * @throws IllegalArgumentException
     *             if actions is {@code null}, empty or contains an action other than the
     *             specified possible actions.
     */
    public DeviceMgmtPermission(String name, String actions) {
        super(name.toString());
        if (null == actions) {
            throw new IllegalArgumentException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_NULL_ACTIONS)
            );
        }
        myActions = ActionFactory.verifyAndOrderActions(actions, this);
    }

    /**
     * Checks two {@code DeviceMgmtPermission} objects for equality.
     *
     * @param obj
     *            the object to test for equality with this object.
     *
     * @return {@code true} if {@code obj} is a {@code DeviceMgmtPermission} and has the same target name and
     *         actions as this {@code DeviceMgmtPermission} object.
     */
    @Override
    public boolean equals(Object obj) {
        return DevicePermission.equals(this, obj);
    }

    /**
     * Returns the list of possible actions in the following order: {@code register},
     * {@code unregister} or {@code open}.
     *
     * @return comma-separated list of possible actions.
     */
    @Override
    public String getActions() {
        return myActions;
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    /**
     * Checks if this {@code DeviceMgmtPermission} object "implies" the specified permission.
     * <p />
     * More specifically, this method returns {@code true} if:
     * <ul>
     * <li>{@code permission} is an instance of {@code DeviceMgmtPermission}, and
     * <li>{@code permission}'s actions are a proper subset of this action list, and</i>
     * <li>{@code permission}'s device name, ID or range thereof
     * is included in this device name or ID range, whichever is defined.
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
        if ((permission == null) || (permission.getClass() != getClass()))
            return false;

        if (!ActionFactory.actionImplies(permission.getActions(), this)) return false;

        int idx = 0;
        String name = permission.getName();
        while (-1 != (idx = name.indexOf(':', idx))) {
            if (idx == 0 || '\\' != name.charAt(idx - 1) ) {
                break;
            }
        }
        String thatName;
        String thatID = "";
        if (-1 == idx) {
            thatName = name;
        } else {
            thatName = name.substring(0, idx);
            thatID = name.substring(idx + 1);
        }

        idx = 0;
        name = getName();
        while (-1 != (idx = name.indexOf(':', idx))) {
            if (idx == 0 || '\\' != name.charAt(idx - 1) ) {
                break;
            }
        }
        String thisName;
        String thisID = "";
        if (-1 == idx) {
            thisName = name;
        } else {
            thisName = name.substring(0, idx);
            thisID = name.substring(idx + 1);
        }

        if (!"*".equals(thisName)) {
            // the empty string ("") designates an undefined peripheral name
            // that may only be matched by an empty string or an asterisk.
            // the same condition is for full name.
            if (!thisName.equals(thatName)) {
                return false;
            }
        }

        if (!"*".equals(thisID) &&
            !(thisID.length() == 0 && thatID.length() == 0)) {
            boolean foundDash = false;
            for (int i = 0; i < thisID.length(); i++) {
                char c = thisID.charAt(i);
                if (!Character.isDigit(c)) {
                     if('-' == c && !foundDash) {
                         foundDash = true;
                         continue;
                     }
                } else {
                    continue;
                }
                // invalid format
                return false;
            }
            int thisLow = 0;
            int thisHigh = Integer.MAX_VALUE;
            if (foundDash) {
                idx = thisID.indexOf('-');
                if (idx > 0) {
                    thisLow = Integer.parseInt(thisID.substring(0, idx));
                }
                if (idx < thisID.length() - 1) {
                    thisHigh = Integer.parseInt(thisID.substring(idx + 1));
                }
            } else {
                thisLow = thisHigh = Integer.parseInt(thisID);
            }

            foundDash = false;
            for (int i = 0; i < thatID.length(); i++) {
                char c = thatID.charAt(i);
                if (!Character.isDigit(c)) {
                     if('-' == c && !foundDash) {
                         foundDash = true;
                         continue;
                     }
                } else {
                    continue;
                }
                // invalid format
                return false;
            }

            int thatLow = 0;
            int thatHigh = Integer.MAX_VALUE;
            if (foundDash) {
                idx = thatID.indexOf('-');
                if (idx > 0) {
                    thatLow = Integer.parseInt(thatID.substring(0, idx));
                }
                if (idx < thatID.length() - 1) {
                    thatHigh = Integer.parseInt(thatID.substring(idx + 1));
                }
            } else {
                thatLow = thatHigh = Integer.parseInt(thatID);
            }

            return (thatLow >= thisLow && thatLow <= thisHigh &&
                thatHigh >= thisLow && thatHigh <= thisHigh);

        }

        return true;

    }

    /**
     * Returns a new {@code PermissionCollection} for storing {@code DeviceMgmtPermission} objects.
     * <p>
     * {@code DeviceMgmtPermission} objects must be stored in a manner that allows them to be inserted into the
     * collection in any order, but that also enables the {@code PermissionCollection} implies method to be implemented
     * in an efficient (and consistent) manner.
     *
     * <p>
     * If {@code null} is returned, then the caller of this method is free to store permissions of this type in any
     * PermissionCollection they choose (one that uses a {@code Hashtable}, one that uses a {@code Vector}, etc).
     *
     * @return a new {@code PermissionCollection} suitable for storing {@code DeviceMgmtPermission} objects, or
     *         {@code null} if one is not defined.
     */
    @Override
    public PermissionCollection newPermissionCollection() {
        return null;
    }
}
