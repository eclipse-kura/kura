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
package org.freedesktop.networkmanager;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class NeedSecretsTuple extends Tuple {

    @Position(0)
    private String username;
    @Position(1)
    private String password;

    public NeedSecretsTuple(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void setUsername(String arg) {
        this.username = arg;
    }

    public String getUsername() {
        return this.username;
    }

    public void setPassword(String arg) {
        this.password = arg;
    }

    public String getPassword() {
        return this.password;
    }

}
