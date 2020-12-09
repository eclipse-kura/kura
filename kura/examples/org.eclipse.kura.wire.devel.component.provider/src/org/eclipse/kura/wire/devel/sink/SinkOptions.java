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
package org.eclipse.kura.wire.devel.sink;

import java.util.Map;

import org.eclipse.kura.wire.devel.Property;

public class SinkOptions {

    private static final Property<Boolean> MEASURE_TIMINGS = new Property<>("measure.timings", true);

    private final Map<String, Object> properties;

    public SinkOptions(final Map<String, Object> properties) {
        this.properties = properties;
    }

    public boolean shouldMeasureTimings() {
        return MEASURE_TIMINGS.get(properties);
    }

}
