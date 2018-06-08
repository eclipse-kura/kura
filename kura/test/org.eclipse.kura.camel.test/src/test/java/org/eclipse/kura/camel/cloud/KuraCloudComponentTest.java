/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.camel.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.Registry;
import org.eclipse.kura.camel.internal.cloud.CloudClientCache;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.core.testutil.TestUtil;
import org.junit.Test;

public class KuraCloudComponentTest {

    @Test
    public void testDoStartNoRegistry() {
        final CamelContext ctxMock = mock(CamelContext.class);

        KuraCloudComponent kcc = new KuraCloudComponent() {

            @Override
            public CamelContext getCamelContext() {
                return ctxMock;
            }
        };

        try {
            kcc.doStart();
            fail("Exception was expected.");
        } catch (IllegalArgumentException e) {
            assertEquals("Registry cannot be null.", e.getMessage());
        } catch (Exception e) {
            fail("This exception was not expected.");
        }
    }

    @Test
    public void testDoStartNullSvc() throws Exception {
        final CamelContext ctxMock = mock(CamelContext.class);
        Registry regMock = mock(Registry.class);
        when(ctxMock.getRegistry()).thenReturn(regMock);

        Class<CloudService> clazz = CloudService.class;
        Set<CloudService> set = new HashSet<CloudService>();
        set.add(null);
        when(regMock.findByType(clazz)).thenReturn(set);

        KuraCloudComponent kcc = new KuraCloudComponent() {

            @Override
            public CamelContext getCamelContext() {
                return ctxMock;
            }
        };

        try {
            kcc.doStart();
            fail("Exception was expected.");
        } catch (IllegalStateException e) {
            assertEquals("'cloudService' is not set and not found in Camel context service registry", e.getMessage());
        } catch (Exception e) {
            fail("This exception was not expected.");
        }

        assertNull(TestUtil.getFieldValue(kcc, "cloudService"));
    }

    @Test
    public void testDoStart() throws Exception {
        final CamelContext ctxMock = mock(CamelContext.class);
        Registry regMock = mock(Registry.class);
        when(ctxMock.getRegistry()).thenReturn(regMock);

        Class<CloudService> clazz = CloudService.class;
        Set<CloudService> set = new HashSet<CloudService>();
        CloudService cs = mock(clazz);
        set.add(cs);
        when(regMock.findByType(clazz)).thenReturn(set);

        KuraCloudComponent kcc = new KuraCloudComponent(ctxMock);

        kcc.doStart();

        assertEquals(cs, TestUtil.getFieldValue(kcc, "cloudService"));
        assertNotNull(TestUtil.getFieldValue(kcc, "cache"));
    }

    @Test
    public void testDoStop() throws Exception {
        final CloudClientCache cacheMock = mock(CloudClientCache.class);

        KuraCloudComponent kcc = new KuraCloudComponent();

        TestUtil.setFieldValue(kcc, "cache", cacheMock);

        assertNotNull(TestUtil.getFieldValue(kcc, "cache"));

        kcc.doStop();

        assertNull(TestUtil.getFieldValue(kcc, "cache"));
        verify(cacheMock, times(1)).close();
    }

    @Test
    public void testCreateEndpointWrongRem() throws Exception {
        KuraCloudComponent kcc = new KuraCloudComponent();

        String uri = "uri";
        String remain = "remain";
        Map<String, Object> parameters = new HashMap<String, Object>();

        try {
            kcc.createEndpoint(uri, remain, parameters);
            fail("Exception was expected.");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Wrong kura-cloud URI format"));
        }
    }

    @Test
    public void testCreateEndpoint() throws Exception {
        KuraCloudComponent kcc = new KuraCloudComponent();
        final CamelContext ctxMock = mock(CamelContext.class);
        kcc.setCamelContext(ctxMock);

        String uri = "uri";
        String remain = "app/topic";
        Map<String, Object> parameters = new HashMap<String, Object>();

        Endpoint endpoint = kcc.createEndpoint(uri, remain, parameters);

        assertNotNull(endpoint);
        assertEquals(uri, endpoint.getEndpointUri());
    }

}
