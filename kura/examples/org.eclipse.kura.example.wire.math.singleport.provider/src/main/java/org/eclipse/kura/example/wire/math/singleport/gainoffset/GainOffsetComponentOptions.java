/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.example.wire.math.singleport.gainoffset;

import java.util.List;
import java.util.Map;

public class GainOffsetComponentOptions {

    private static final String CONFIGURATION_PROP_NAME = "configuration";
    private static final String EMIT_RECEIVED_PROPERTIES_PROP_NAME = "emit.received.properties";

    private static final boolean EMIT_RECEIVED_PROPERTIES_DEFAULT = false;
    private static final String CONFIGURATION_DEFAULT = "";

    private List<GainOffsetEntry> entries;
    private boolean emitReceivedProperties;

    public GainOffsetComponentOptions(final Map<String, Object> properties) {
        this.emitReceivedProperties = getSafe(properties.get(EMIT_RECEIVED_PROPERTIES_PROP_NAME),
                EMIT_RECEIVED_PROPERTIES_DEFAULT);
        this.entries = GainOffsetEntry
                .parseAll(getSafe(properties.get(CONFIGURATION_PROP_NAME), CONFIGURATION_DEFAULT));
    }

    public List<GainOffsetEntry> getEntries() {
        return entries;
    }

    public boolean shouldEmitReceivedProperties() {
        return emitReceivedProperties;
    }

    @SuppressWarnings("unchecked")
    private <T> T getSafe(Object o, T defaultValue) {
        if (defaultValue.getClass().isInstance(o)) {
            return (T) o;
        }
        return defaultValue;
    }
}
