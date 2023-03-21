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

import java.util.List;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class LoadConnectionsTuple extends Tuple {

    @Position(0)
    private boolean status;
    @Position(1)
    private List<String> failures;

    public LoadConnectionsTuple(boolean status, List<String> failures) {
        this.status = status;
        this.failures = failures;
    }

    public void setStatus(boolean arg) {
        this.status = arg;
    }

    public boolean getStatus() {
        return this.status;
    }

    public void setFailures(List<String> arg) {
        this.failures = arg;
    }

    public List<String> getFailures() {
        return this.failures;
    }

}
