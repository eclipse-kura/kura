/*******************************************************************************
 * Copyright (c) 2017, 2022 Eurotech and/or its affiliates and others
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

import java.util.List;

import org.eclipse.kura.wire.WireRecord;

import jdk.nashorn.api.scripting.AbstractJSObject;

/**
 * @deprecated as of Kura 5.3.0
 */
@Deprecated
class WireRecordListWrapper extends AbstractJSObject {

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

    @Override
    public void setMember(String name, Object value) {
        throw new UnsupportedOperationException("This object is immutable");
    }

    @Override
    public void setSlot(int index, Object value) {
        throw new UnsupportedOperationException("This object is immutable");
    }

    @Override
    public void removeMember(String name) {
        throw new UnsupportedOperationException("This object is immutable");
    }
}
