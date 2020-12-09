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
package org.eclipse.kura.internal.wire.conditional;

import java.util.List;

import org.eclipse.kura.wire.WireRecord;

class WireRecordListWrapper extends ImmutableJSObject {

    private static final String LENGTH_PROP_NAME = "length";
    private final List<WireRecord> records;

    public WireRecordListWrapper(List<WireRecord> records) {
        this.records = records;
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public boolean hasMember(String name) {
        return LENGTH_PROP_NAME.equals(name);
    }

    @Override
    public Object getMember(String name) {
        if (LENGTH_PROP_NAME.equals(name)) {
            return this.records.size();
        }
        return null;
    }

    @Override
    public boolean hasSlot(int slot) {
        return slot >= 0 && slot < this.records.size();
    }

    @Override
    public Object getSlot(int index) {
        if (!hasSlot(index)) {
            return null;
        }
        return new WireRecordWrapper(this.records.get(index).getProperties());
    }

}
