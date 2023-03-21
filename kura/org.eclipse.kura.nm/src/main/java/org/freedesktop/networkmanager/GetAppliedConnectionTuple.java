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

import java.util.Map;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt64;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
public class GetAppliedConnectionTuple extends Tuple {

    @Position(0)
    private Map<String, Map<String, Variant<?>>> connection;
    @Position(1)
    private UInt64 versionId;

    public GetAppliedConnectionTuple(Map<String, Map<String, Variant<?>>> connection, UInt64 versionId) {
        this.connection = connection;
        this.versionId = versionId;
    }

    public void setConnection(Map<String, Map<String, Variant<?>>> arg) {
        this.connection = arg;
    }

    public Map<String, Map<String, Variant<?>>> getConnection() {
        return this.connection;
    }

    public void setVersionId(UInt64 arg) {
        this.versionId = arg;
    }

    public UInt64 getVersionId() {
        return this.versionId;
    }

}
