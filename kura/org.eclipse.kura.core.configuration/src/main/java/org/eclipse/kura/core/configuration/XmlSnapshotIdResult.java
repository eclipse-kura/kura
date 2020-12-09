/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.configuration;

import java.util.List;

/**
 * Utility class to serialize a set of snapshot ids.
 */
public class XmlSnapshotIdResult {

    private List<Long> snapshotIds;

    public XmlSnapshotIdResult() {
    }

    public List<Long> getSnapshotIds() {
        return this.snapshotIds;
    }

    public void setSnapshotIds(List<Long> snapshotIds) {
        this.snapshotIds = snapshotIds;
    }
}
