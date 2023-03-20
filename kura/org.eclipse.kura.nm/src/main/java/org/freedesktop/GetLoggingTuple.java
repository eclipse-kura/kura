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
package org.freedesktop;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class GetLoggingTuple extends Tuple {

    @Position(0)
    private String level;
    @Position(1)
    private String domains;

    public GetLoggingTuple(String level, String domains) {
        this.level = level;
        this.domains = domains;
    }

    public void setLevel(String arg) {
        this.level = arg;
    }

    public String getLevel() {
        return this.level;
    }

    public void setDomains(String arg) {
        this.domains = arg;
    }

    public String getDomains() {
        return this.domains;
    }

}
