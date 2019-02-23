/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
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
