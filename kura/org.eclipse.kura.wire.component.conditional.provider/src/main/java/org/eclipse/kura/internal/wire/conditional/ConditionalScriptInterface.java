/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.internal.wire.conditional;

import java.util.List;

import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireRecord;

public class ConditionalScriptInterface {

    private List<WireRecord> inputRecords;

    public ConditionalScriptInterface(WireEnvelope inputWireEnvelope) {
        if (inputWireEnvelope != null) {
            this.inputRecords = inputWireEnvelope.getRecords();
        }
    }

    public Object getInputRecord(int i, String name) {
        if (i < 0 || i > this.inputRecords.size()) {
            return null;
        }
        return this.inputRecords.get(i).getProperties().get(name).getValue();
    }
}