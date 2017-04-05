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
package dio.gpio;

import dio.policy.DIOPermissionCollection;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Policy;
import jdk.dio.DeviceMgmtPermission;
import jdk.dio.gpio.GPIOPinPermission;
import jdk.dio.gpio.GPIOPortPermission;

/**
 * @title Custom gpio Policy
 * @author stanislav.smirnov@oracle.com
 */
public class GPIOPolicy extends Policy {

    private static PermissionCollection permissions;

    public GPIOPolicy(DeviceMgmtPermission deviceMgmtPermission,
            GPIOPinPermission pinPermission,
            GPIOPortPermission portPermission) {
        super();
        init(deviceMgmtPermission, pinPermission, portPermission);

    }

    private void init(DeviceMgmtPermission deviceMgmtPermission,
            GPIOPinPermission pinPermission,
            GPIOPortPermission portPermission) {

        if (permissions == null) {
            permissions = new DIOPermissionCollection();
        }

        addDeviceMgmtPermission(deviceMgmtPermission);

        addPinPermission(pinPermission);

        addPortPermission(portPermission);

    }

    public void addDeviceMgmtPermission(DeviceMgmtPermission deviceMgmtPermission) {
        if (deviceMgmtPermission != null) {
            permissions.add(deviceMgmtPermission);
        }
    }

    public void addPinPermission(GPIOPinPermission pinPermission) {
        if (pinPermission != null) {
            permissions.add(pinPermission);
        }
    }

    public void addPortPermission(GPIOPortPermission portPermission) {
        if (portPermission != null) {
            permissions.add(portPermission);
        }
    }

    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
        return permissions;
    }
}
