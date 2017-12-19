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