/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.wire.script.filter.provider;

import java.util.List;

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.script.filter.localization.ScriptFilterMessages;

import jdk.nashorn.api.scripting.AbstractJSObject;

class WireRecordListWrapper extends AbstractJSObject {

    private static final ScriptFilterMessages messages = LocalizationAdapter.adapt(ScriptFilterMessages.class);

    private static final String LENGHT_PROP_NAME = "lenght";
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
        return LENGHT_PROP_NAME.equals(name);
    }

    @Override
    public Object getMember(String name) {
        if ("length".equals(name)) {
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
        throw new UnsupportedOperationException(messages.errorObjectImmutable());
    }

    @Override
    public void setSlot(int index, Object value) {
        throw new UnsupportedOperationException(messages.errorObjectImmutable());
    }

    @Override
    public void removeMember(String name) {
        throw new UnsupportedOperationException(messages.errorObjectImmutable());
    }
}
