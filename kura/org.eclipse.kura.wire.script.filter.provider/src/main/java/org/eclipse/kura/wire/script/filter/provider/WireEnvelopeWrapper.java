/*******************************************************************************
 * Copyright (c) 2017, 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.wire.script.filter.provider;

public class WireEnvelopeWrapper {

    private final String emitterPid;
    private final WireRecordListWrapper records;

    WireEnvelopeWrapper(WireRecordListWrapper records, String emitterPid) {
        this.records = records;
        this.emitterPid = emitterPid;
    }

    public String getEmitterPid() {
        return this.emitterPid;
    }

    public WireRecordListWrapper getRecords() {
        return this.records;
    }

}
