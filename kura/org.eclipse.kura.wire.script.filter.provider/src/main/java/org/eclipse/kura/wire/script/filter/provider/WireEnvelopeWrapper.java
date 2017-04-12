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

import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.wire.script.filter.localization.ScriptFilterMessages;

import jdk.nashorn.api.scripting.AbstractJSObject;

class WireEnvelopeWrapper extends AbstractJSObject {

    private static final ScriptFilterMessages messages = LocalizationAdapter.adapt(ScriptFilterMessages.class);

    private static final String RECORDS_PROP_NAME = "records";
    private static final String EMITTER_PID_PROP_NAME = "emitterPid";

    String emitterPid;
    WireRecordListWrapper records;

    WireEnvelopeWrapper(WireRecordListWrapper records, String emitterPid) {
        this.records = records;
        this.emitterPid = emitterPid;
    }

    @Override
    public boolean hasMember(String name) {
        return EMITTER_PID_PROP_NAME.equals(name) || RECORDS_PROP_NAME.equals(name);
    }

    @Override
    public Object getMember(String name) {
        if (EMITTER_PID_PROP_NAME.equals(name)) {
            return this.emitterPid;
        } else if (RECORDS_PROP_NAME.equals(name)) {
            return this.records;
        }
        return null;
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
