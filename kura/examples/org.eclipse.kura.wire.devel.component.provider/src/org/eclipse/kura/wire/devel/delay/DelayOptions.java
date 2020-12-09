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
package org.eclipse.kura.wire.devel.delay;

import java.util.Map;

import org.eclipse.kura.wire.devel.Property;

public class DelayOptions {

    private static final Property<Integer> DELAY_AVERAGE = new Property<>("delay.average", 1000);
    private static final Property<Integer> DELAY_STD_DEV = new Property<>("delay.std.dev", 100);

    private final Map<String, Object> properties;

    public DelayOptions(final Map<String, Object> properties) {
        this.properties = properties;
    }

    public int getAverageDelay() {
        return DELAY_AVERAGE.get(properties);
    }

    public int getDelayStdDev() {
        return DELAY_STD_DEV.get(properties);
    }
}
