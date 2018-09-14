/*******************************************************************************
 * Copyright (c) 2016, 2018 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.camel.type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.camel.component.AbstractRouterTest;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireRecord;
import org.junit.Test;

public class TypeConverterTest extends AbstractRouterTest {

    private <T> T convertTo(Class<T> clazz, Object value) {
        return getCamelContext().getTypeConverter().convertTo(clazz, value);
    }

    @Test
    public void testFromMap() throws InterruptedException {
        final KuraPayload result = convertTo(KuraPayload.class, Collections.singletonMap("foo", "bar"));

        assertNotNull(result);
        assertNotNull(result.getTimestamp());

        assertEquals("bar", result.getMetric("foo"));
    }

    @Test
    public void testRecordToMap() {

        final Map<?, ?> result = convertTo(Map.class, new WireRecord[] { createDefaultRecord() });

        assertNotNull(result);
        assertEquals(createDefaultRecordExpected(), result);

    }

    @Test
    public void testRecordsToMap() {

        final Map<?, ?> result = convertTo(Map.class, createDefaultRecord());

        assertNotNull(result);
        assertEquals(createDefaultRecordExpected(), result);

    }

    private WireRecord createDefaultRecord() {

        final Map<String, TypedValue<?>> values = new HashMap<>();

        values.put("FOO", TypedValues.newTypedValue("bar"));
        values.put("BAR", TypedValues.newTypedValue(42));

        return new WireRecord(values);

    }

    private Map<String, Object> createDefaultRecordExpected() {

        final Map<String, Object> expected = new HashMap<>();

        expected.put("FOO", "bar");
        expected.put("BAR", 42);

        return expected;

    }
}
