/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *   
 *   Contributors:
 *      Eurotech
 *      Amit Kumar Mondal
 ******************************************************************************/
package org.eclipse.kura.internal.wire;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.eclipse.kura.internal.wire.WireConstants.EMITTER_POSTFIX;
import static org.eclipse.kura.internal.wire.WireConstants.RECEIVER_POSTFIX;
import static org.eclipse.kura.internal.wire.WireConstants.FILTER_POSTFIX;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.wire.WireConfiguration;
import org.junit.Test;

public class WireServiceOptionsTest {

    @Test
    public void testGetInstanceEmpty() {
        Map<String, Object> properties = new HashMap<String, Object>();

        WireServiceOptions options = WireServiceOptions.getInstance(properties);

        assertNotNull(options.getWireConfigurations());
        assertEquals(0, options.getWireConfigurations().size());
    }

    @Test
    public void testGetInstanceNoSeparator() {
        Map<String, Object> properties = new HashMap<String, Object>();

        properties.put("testKey", "testValue");

        WireServiceOptions options = WireServiceOptions.getInstance(properties);

        assertNotNull(options.getWireConfigurations());
        assertEquals(0, options.getWireConfigurations().size());
    }

    @Test
    public void testGetInstancePartialSeparator() {
        Map<String, Object> properties = new HashMap<String, Object>();

        properties.put("1." + EMITTER_POSTFIX.value(), "emitterPid"); // 1.emitter
        properties.put("1." + RECEIVER_POSTFIX.value(), "receiverPid"); // 1.receiver
        properties.put("1filter", "filter");

        WireServiceOptions options = WireServiceOptions.getInstance(properties);

        List<WireConfiguration> configurations = options.getWireConfigurations();
        assertNotNull(configurations);
        assertEquals(1, configurations.size());
        WireConfiguration cfg = configurations.get(0);
        assertEquals("emitterPid", cfg.getEmitterPid());
        assertEquals("receiverPid", cfg.getReceiverPid());
        assertNull(cfg.getFilter());
    }

    @Test
    public void testGetInstance() {

        Map<String, Object> properties = new HashMap<String, Object>();

        properties.put("1." + EMITTER_POSTFIX.value(), "emitterPid"); // 1.emitter
        properties.put("1." + RECEIVER_POSTFIX.value(), "receiverPid"); // 1.receiver
        properties.put("1." + FILTER_POSTFIX.value(), "filter"); // 1.filter

        WireServiceOptions options = WireServiceOptions.getInstance(properties);

        List<WireConfiguration> configurations = options.getWireConfigurations();
        assertNotNull(configurations);
        assertEquals(1, configurations.size());
        WireConfiguration cfg = configurations.get(0);
        assertEquals("emitterPid", cfg.getEmitterPid());
        assertEquals("receiverPid", cfg.getReceiverPid());
        assertEquals("filter", cfg.getFilter());
    }
}
