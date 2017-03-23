/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.camel.bean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.eclipse.kura.camel.bean.PayloadFactory;
import org.eclipse.kura.message.KuraPayload;
import org.junit.Test;


public class PayloadFactoryTest {

    @Test
    public void testCreateTimestamp() {
        PayloadFactory pf = new PayloadFactory();

        Date timestamp = new Date();

        KuraPayload payload = pf.create(timestamp);

        assertNotNull(payload);
        assertNotNull(payload.getTimestamp());
        assertEquals(timestamp, payload.getTimestamp());
    }

    @Test
    public void testCreateKVP() {
        PayloadFactory pf = new PayloadFactory();

        String key = "key";
        Object value = "val";

        KuraPayload payload = pf.create(key, value);

        assertNotNull(payload);
        assertNotNull(payload.getTimestamp());
        assertTrue(new Date().getTime() >= payload.getTimestamp().getTime());

        Object metric = payload.getMetric(key);
        assertNotNull(metric);
        assertEquals(value, metric);
    }

    @Test
    public void testAppendKVP() {
        PayloadFactory pf = new PayloadFactory();

        String key = "key";
        Object value = "val";
        KuraPayload payload = new KuraPayload();

        KuraPayload payload2 = pf.append(payload, key, value);

        assertNotNull(payload);
        assertNotNull(payload2);
        assertEquals(payload, payload2);
        assertNull(payload.getTimestamp());

        Object metric = payload.getMetric(key);
        assertNotNull(metric);
        assertEquals(value, metric);
    }
}
