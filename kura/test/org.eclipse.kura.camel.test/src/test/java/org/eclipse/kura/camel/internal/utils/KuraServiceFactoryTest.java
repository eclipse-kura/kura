/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.camel.internal.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.apache.camel.spi.Registry;
import org.junit.Test;

public class KuraServiceFactoryTest {

    @Test
    public void testNull() {
        Class<KuraServiceFactoryTest> clazz = KuraServiceFactoryTest.class;

        try {
            KuraServiceFactory.retrieveService(clazz, null);
            fail("Exception was expected.");
        } catch (Exception e) {
            assertEquals("Registry cannot be null.", e.getMessage());
        }
    }

    @Test
    public void testOther() {
        Registry regMock = mock(Registry.class);

        Class<KuraServiceFactoryTest> clazz = KuraServiceFactoryTest.class;

        try {
            KuraServiceFactory.retrieveService(clazz, regMock);
            fail("Exception was expected.");
        } catch (Exception e) {
            assertEquals("No " + clazz.getCanonicalName() + " service instance found in a registry.", e.getMessage());
        }
    }

    @Test
    public void testMulti() {
        Class<KuraServiceFactoryTest> clazz = KuraServiceFactoryTest.class;

        Registry regMock = mock(Registry.class);
        Set<KuraServiceFactoryTest> set = new HashSet<KuraServiceFactoryTest>();
        set.add(new KuraServiceFactoryTest());
        set.add(new KuraServiceFactoryTest());
        when(regMock.findByType(clazz)).thenReturn(set);

        try {
            KuraServiceFactory.retrieveService(clazz, regMock);
            fail("Exception was expected.");
        } catch (Exception e) {
            assertTrue(e.getMessage().startsWith("Too many"));
        }
    }

    @Test
    public void testNull2() {
        Class<KuraServiceFactoryTest> clazz = KuraServiceFactoryTest.class;

        Registry regMock = mock(Registry.class);
        Set<KuraServiceFactoryTest> set = new HashSet<KuraServiceFactoryTest>();
        set.add(null);
        when(regMock.findByType(clazz)).thenReturn(set);

        KuraServiceFactoryTest service = KuraServiceFactory.retrieveService(clazz, regMock);
        assertNull(service);
    }

    @Test
    public void testSingle() {
        Class<KuraServiceFactoryTest> clazz = KuraServiceFactoryTest.class;

        Registry regMock = mock(Registry.class);
        Set<KuraServiceFactoryTest> set = new HashSet<KuraServiceFactoryTest>();
        KuraServiceFactoryTest tst = new KuraServiceFactoryTest();
        set.add(tst);
        when(regMock.findByType(clazz)).thenReturn(set);

        KuraServiceFactoryTest service = KuraServiceFactory.retrieveService(clazz, regMock);
        assertEquals(tst, service);
    }

}
