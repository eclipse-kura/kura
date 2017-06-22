/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.camel.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;


public class ConfigurationTest {

    @Test
    public void testBoolean() {
        String key = "key";

        assertFalse(Configuration.asBoolean(null, key));

        assertTrue(Configuration.asBoolean(null, key, true));

        Map<String, Object> properties = new HashMap<String, Object>();
        assertTrue(Configuration.asBoolean(properties, key, true));

        properties.put(key, Boolean.FALSE);
        assertFalse(Configuration.asBoolean(properties, key, true));

        assertTrue(Configuration.asBoolean(null, key, Boolean.TRUE));

        properties = new HashMap<String, Object>();
        assertTrue(Configuration.asBoolean(properties, key, Boolean.TRUE));

        properties.put(key, Boolean.FALSE);
        assertFalse(Configuration.asBoolean(properties, key, Boolean.TRUE));
    }

    @Test
    public void testDouble() {
        String key = "key";
        double eps = 0.000001;

        assertNull(Configuration.asDouble(null, key));

        assertEquals(1.0, Configuration.asDouble(null, key, 1.0), eps);

        Map<String, Object> properties = new HashMap<String, Object>();
        assertEquals(1.0, Configuration.asDouble(properties, key, 1.0), eps);

        Double one = new Double(1.0);
        properties.put(key, one);
        assertEquals(1.0, Configuration.asDouble(properties, key, 0.0), eps);

        assertEquals(one, Configuration.asDouble(null, key, one));

        properties = new HashMap<String, Object>();
        assertEquals(one, Configuration.asDouble(properties, key, one));

        properties.put(key, 0.0);
        assertEquals(new Double(0.0), Configuration.asDouble(properties, key, one));
    }

    @Test
    public void testInt() {
        String key = "key";

        assertEquals(1, Configuration.asInt(null, key, 1));

        Map<String, Object> properties = new HashMap<String, Object>();
        assertEquals(1, Configuration.asInt(properties, key, 1));

        Integer one = new Integer(1);
        properties.put(key, one);
        assertEquals(1, Configuration.asInt(properties, key, 0));
    }

    @Test
    public void testInteger() {
        String key = "key";
        Integer one = new Integer(1);

        assertNull(Configuration.asInteger(null, key));

        assertEquals(one, Configuration.asInteger(null, key, one));

        Map<String, Object> properties = new HashMap<String, Object>();
        assertEquals(new Integer(1), Configuration.asInteger(properties, key, 1));

        properties.put(key, one);
        assertEquals(one, Configuration.asInteger(properties, key, 0));
    }

    @Test
    public void testLong() {
        String key = "key";

        assertNull(Configuration.asLong(null, key));

        assertEquals(1, Configuration.asLong(null, key, 1));

        Map<String, Object> properties = new HashMap<String, Object>();
        assertEquals(1, Configuration.asLong(properties, key, 1));

        Long one = new Long(1);
        properties.put(key, one);
        assertEquals(1, Configuration.asLong(properties, key, 0));

        assertEquals(one, Configuration.asLong(null, key, one));

        properties = new HashMap<String, Object>();
        assertEquals(one, Configuration.asLong(properties, key, one));

        properties.put(key, 0);
        assertEquals(new Long(0), Configuration.asLong(properties, key, one));
    }

    @Test
    public void testString() {
        String key = "key";

        assertNull(Configuration.asString(null, key));

        String defVal = "str";
        assertEquals(defVal, Configuration.asString(null, key, defVal));

        Map<String, Object> properties = new HashMap<String, Object>();
        assertEquals(defVal, Configuration.asString(properties, key, defVal));

        String str = "other string";
        properties.put(key, str);
        assertEquals(str, Configuration.asString(properties, key, defVal));
    }

    @Test
    public void testStringNotEmpty() {
        String key = "key";
        String defVal = "str";

        assertEquals(defVal, Configuration.asStringNotEmpty(null, key, defVal));

        Map<String, Object> properties = new HashMap<String, Object>();
        assertEquals(defVal, Configuration.asStringNotEmpty(properties, key, defVal));

        properties.put(key, new Object());
        assertEquals(defVal, Configuration.asStringNotEmpty(properties, key, defVal));

        properties.put(key, key);
        assertEquals(key, Configuration.asStringNotEmpty(properties, key, defVal));

        properties.put(key, "");
        assertEquals(defVal, Configuration.asStringNotEmpty(properties, key, defVal));
    }

}
