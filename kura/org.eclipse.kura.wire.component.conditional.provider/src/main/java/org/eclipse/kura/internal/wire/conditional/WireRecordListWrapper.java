/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
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
