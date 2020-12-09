/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
