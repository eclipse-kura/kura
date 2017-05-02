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
import java.util.Enumeration;
import java.util.Vector;
import com.oracle.dio.utils.ExceptionMessage;
import com.oracle.dio.utils.ActionFactory;

/**
 * The {@code DevicePermission} abstract class is the superclass of all device permissions.
 * <p />
 * A {@code DevicePermission} permission has a target name and, optionally, a list of actions.
 * <p />
 * The target name contains hardware addressing information. It takes the following form:
 * <blockquote> <code>( {controller-spec} ) [ ":" {channel-spec}]</code> </blockquote>
 * <dl>
 * <dt><code>{controller-spec}</code></dt>
 * <dd>The <code>{controller-spec}</code> takes the following form: <blockquote>
 * <code>{controller-name-spec} | {controller-number} | "*" | ""</code> </blockquote>
 * <dl>
 * <dt><code>{controller-name-spec}</code></dt>
 * <dd>The <code>{controller-name-spec}</code> string is the string representation of a controller name as
 * may be returned by a call to {@link DeviceConfig.HardwareAddressing#getControllerName
 * DeviceConfig.HardwareAddressing.getControllerName}. A controller name is Operating System specific
 * such as a <em>device file</em> name on UNIX systems. Occurrences of the semicolon character (
 * {@code ":"}) must be escaped with a backslash ({@code "\"}). A <code>{controller-name-spec}</code>
 * string that ends with an asterisk ({@code "*"}) is a prefix pattern that matches all the controller
 * names starting with the same prefix.</dd>
 * <dt><code>{controller-number}</code></dt>
 * <dd>The <code>{controller-number}</code> string is the decimal string representation of a controller
 * number as may be returned by a call to
 * {@link DeviceConfig.HardwareAddressing#getControllerNumber
 * DeviceConfig.HardwareAddressing.getControllerNumber}. The characters in the string must all be
 * decimal digits.</dd>
 * </dl>
 * A <code>{controller-spec}</code> specification consisting of the asterisk ({@code "*"}) matches all
 * controller names or numbers. A <code>{controller-spec}</code> specification consisting of the empty
 * string ({@code ""}) designates an undefined controller name or number that may only be matched by an
 * empty string or an asterisk.</dd>
 * <dt>{channel-spec}</dt>
 * <dd>The <code>{channel-spec}</code> takes the following form: <blockquote>
 * <code>{channel-desc} | "*" | ""</code> </blockquote>
 * <dl>
 * <dt><code>{channel-desc}</code></dt>
 * <dd>The <code>{channel-desc}</code> string is device type-specific and must be defined by
 * subclasses.</dd>
 * </dl>
 * A <code>{channel-spec}</code> specification consisting of the asterisk ({@code "*"}) matches all
 * channels. A <code>{channel-spec}</code> specification consisting of the empty string ({@code ""})
 * designates an undefined channel that may only be matched by an empty string or an asterisk.</dd>
 * </dl>
 * Subclasses of {@code DevicePermission} may defined additional specific target name formats to
 * designate devices using their specific hardware addressing information.
 * <p />
 * The actions to be granted are passed to the constructor in a string containing a list of one or
 * more comma-separated keywords. The supported common actions are {@code open} and
 * {@code powermanage}. Their meaning is defined as follows:
 * <dl>
 * <dt>{@code open}</dt>
 * <dd>open a device (see {@link DeviceManager#open DeviceManager.open})</dd>
 * <dt>{@code powermanage}</dt>
 * <dd>manage the power saving mode of a device (see
 * {@link jdk.dio.power.PowerManaged})</dd>
 * </dl>
 * </dd> </dl> Additional actions to be granted may be defined by subclasses of
 * {@code DevicePermission}.
 *
 * @see DeviceManager#open DeviceManager.open
 * @see jdk.dio.power.PowerManaged
 * @since 1.0
 */
public abstract class DevicePermission extends Permission {
    /**
     * The {@code open} action.
     */
    public static final String OPEN = "open";

    /**
     * The {@code powermanage} action.
     */
    public static final String POWER_MANAGE = "powermanage";

    /**
     * Comam-separated action list
     *
     */
    private String myActions;

    /**
     * Constructs a new {@code DevicePermission} with the specified
     * target name and the implicit {@code open} action.
     *
     * @param name
     *            the target name (as defined above).
     * @throws NullPointerException
     *             if {@code name} is {@code null}.
     * @see #getName getName
     */
    public DevicePermission(String name) {
        super(checkName(name));
        myActions = OPEN;
    }

    private static String checkName(String name) {
        if(name == null){
            throw new NullPointerException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_NULL_NAME)
            );
        }

        return name;
    }

    /**
     * Constructs a new {@code DevicePermission} instance with the specified target name and
     * action list.
     *
     * @param name
     *            the target name (as defined above).
     * @param actions
     *            comma-separated list of device operations: {@code open} or {@code powermanage}
     *            (additional actions may be defined by subclasses).
     * @throws NullPointerException
     *             if {@code name} is {@code null}.
     * @throws IllegalArgumentException
     *             if actions is {@code null}, empty or contains an
     *             action other than the specified possible actions.
     * @see #getName getName
     */
    public DevicePermission(String name, String actions) {
        super(checkName(name));
        if(actions == null){
            throw new IllegalArgumentException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_NULL_ACTIONS)
            );
        }
        if(actions.length() == 0){
            throw new IllegalArgumentException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_EMPTY_ACTIONS)
            );
        }

        myActions = ActionFactory.verifyAndOrderActions(actions, this);
    }

    /**
     * Checks two {@code DevicePermission} objects for equality.
     *
     * @param obj
     *            the object to test for equality with this object.
     * @return {@code true} if {@code obj} is a {@code DevicePermission} and has the same target
     *         name and actions as this {@code DevicePermission} object.
     */
    @Override
    public boolean equals(Object obj) {
        return equals(this, obj);
    }

    /**
     * Static fucntion for utilization by this class and {@link
     * DeviceMgmtPermission}
     *
     * @param obj1 First object to compare
     * @param obj2 Second object to compare
     *
     * @return {@code true} if objects are equals, {@code false}
     *         otherwise
     */
    static boolean equals(Object obj1, Object obj2) {
        if (obj1 == obj2)
            return true;

        if ((obj1 == null) || (obj2 == null) || (obj1.getClass() != obj2.getClass()))
            return false;

        Permission p1 = (Permission) obj1;
        Permission p2 = (Permission) obj2;

        return (p1.getName().equals(p2.getName()) && p1.getActions().equals(p2.getActions()));
    }

    /**
     * Returns the list of possible actions in the following order: {@code open} or
     * {@code powermanage} (additional actions may be defined by subclasses).
     *
     * @return comma-delimited list of possible actions.
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
     * Checks if this {@code DevicePermission} object "implies" the specified permission.
     * <p />
     * More specifically, this method returns {@code true} if:
     * <ul>
     * <li>{@code permission} is an instance of {@code DevicePermission}, and</li>
     * <li>{@code permission}'s actions are a proper subset of this object's action list, and</i></li>
     * <li>{@code permission}'s hardware addressing information or range thereof is included in this
     * {@code DevicePermission}'s hardware addressing information range.</li>
     * </ul>
     *
     * @param permission
     *            the permission to check against.
     * @return {@code true} if the specified permission is not {@code null} and is implied by this
     *         object, {@code false} otherwise.
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
        String thatDevice;
        String thatChannel = "";
        if (-1 == idx) {
            thatDevice = name;
        } else {
            thatDevice = name.substring(0, idx);
            thatChannel = name.substring(idx + 1);
        }

        idx = 0;
        name = getName();
        while (-1 != (idx = name.indexOf(':', idx))) {
            if (idx == 0 || '\\' != name.charAt(idx - 1) ) {
                break;
            }
        }

        String thisDevice;
        String thisChannel = "";
        if (-1 == idx) {
            thisDevice = name;
        } else {
            thisDevice = name.substring(0, idx);
            thisChannel = name.substring(idx + 1);
        }


        // compare names
        if (!"*".equals(thisDevice)) { // if not pure wildcard
            if (thisDevice.endsWith("*")) {
                // compare "\dev\tty*" and "\dev\*" or "\dev\tty1" and "\dev\*"
                if (thisDevice.length() > thatDevice.length() || // wildcard has to be shorter or equals to other name or whildcard
                    !thatDevice.startsWith(thisDevice.substring(0, thisDevice.length()-1)) // other name should starts with our wildcard
                    ) {
                    return false;
                }
            } else {
                if (!thisDevice.equals(thatDevice)) { // other name may not be either wildcard or different name
                    return false;
                }
            }
        }

        if (!"*".equals(thisChannel)) {
            // compare channels
            if (0 == thisChannel.length() && (0 == thatChannel.length() || "*".equals(thatChannel))) {
                //A {channel-spec} specification consisting of the empty string ("") designates an undefined channel
                //that may only be matched by an empty string or an asterisk.
                return true;
            }
            // no need to parse as every {channel-spec} limits characters map to either decimal or heximal digits but does not allow to mix them
            return thisChannel.equals(thatChannel);
        }
        //A {channel-spec} specification consisting of the asterisk ("*") matches all channels.
        return true;
    }

    public String toString(){
        return getClass().getName() + " \'" + getName() + "\' " + getActions();
    }

    /**
     * Returns a new {@code PermissionCollection} for storing {@code DevicePermission} objects.
     * <p>
     * {@code DevicePermission} objects must be stored in a manner that allows them to be
     * inserted into the collection in any order, but that also enables the
     * {@code PermissionCollection} implies method to be implemented in an efficient (and
     * consistent) manner.
     * <p>
     * If {@code null} is returned, then the caller of this method is free to store permissions of
     * this type in any PermissionCollection they choose (one that uses a {@code Hashtable}, one
     * that uses a {@code Vector}, etc).
     *
     * @return a new {@code PermissionCollection} suitable for storing {@code DevicePermission}
     *         objects, or {@code null} if one is not defined.
     */
    @Override
    public PermissionCollection newPermissionCollection() {
        return new PeripheralPermissionCollection();
    }
}

final class PeripheralPermissionCollection extends PermissionCollection {

    private final Vector<DevicePermission> permissions = new Vector<>(6);

    public boolean implies(Permission permission) {
        if (! (permission instanceof DevicePermission)) {
          return false;
        }
        DevicePermission perm = (DevicePermission) permission;
        Enumeration<DevicePermission> search = permissions.elements();
        while (search.hasMoreElements()) {
            if (search.nextElement().implies(perm)) {
                return true;
            }
        }
        return false;
    }
    public void add(Permission permission) {
        if (! (permission instanceof DevicePermission))
            throw new IllegalArgumentException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_INVALID_PERMISSION, permission)
            );
        if (isReadOnly()) {
            throw new SecurityException(
                ExceptionMessage.format(ExceptionMessage.DEVICE_READONLY_PERMISSION_COLLECTION)
            );
        }

        permissions.addElement((DevicePermission)permission);
    }
    public Enumeration elements() {
        return permissions.elements();
    }
}
