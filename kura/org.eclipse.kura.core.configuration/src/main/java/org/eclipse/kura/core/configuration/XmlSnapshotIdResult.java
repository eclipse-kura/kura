/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
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
