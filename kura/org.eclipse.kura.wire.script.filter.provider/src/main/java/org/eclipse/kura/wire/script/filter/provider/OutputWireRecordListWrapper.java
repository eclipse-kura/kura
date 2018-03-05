/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.wire.script.filter.provider;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.wire.WireRecord;

public class OutputWireRecordListWrapper {

    private List<WireRecord> records;

    public void add(Object wrapper) {
        requireNonNull(wrapper, "Added object cannot be null");
        if (!(wrapper instanceof WireRecordWrapper)) {
            throw new IllegalArgumentException("Added object must be a WireRecord");
        }
        if (this.records == null) {
            this.records = new ArrayList<>();
        }
        this.records.add(new WireRecord(((WireRecordWrapper) wrapper).properties));
    }

    List<WireRecord> getRecords() {
        return this.records;
    }
}
