/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.camel.camelcloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutorService;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.ExecutorServiceManager;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.camel.internal.camelcloud.CamelCloudClient;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.core.testutil.TestUtil;
import org.junit.Test;

/*
 * Unit test - no point in integration tests as it is not registered as a service.
 */
public class DefaultCamelCloudServiceTest {

    @Test
    public void testNewClientDefaultEndpointRelease() throws KuraException, NoSuchFieldException {
        CamelContext camelContextMock = mock(CamelContext.class);
        ExecutorServiceManager esmMock = mock(ExecutorServiceManager.class);
        ExecutorService esMock = mock(ExecutorService.class);
        when(esmMock.newThreadPool(anyObject(), anyString(), anyInt(), anyInt())).thenReturn(esMock);
        when(camelContextMock.getExecutorServiceManager()).thenReturn(esmMock);

        DefaultCamelCloudService dccs = new DefaultCamelCloudService(camelContextMock);

        String applicationId = "app";
        CloudClient cloudClient = dccs.newCloudClient(applicationId);

        assertNotNull(cloudClient);
        assertTrue(cloudClient instanceof CamelCloudClient);
        assertEquals(cloudClient.getApplicationId(), applicationId);

        String endp = (String) TestUtil.getFieldValue(cloudClient, "baseEndpoint");
        assertEquals("vm:%s", endp);

        // check registered clients
        String[] identifiers = dccs.getCloudApplicationIdentifiers();
        assertEquals(1, identifiers.length);
        assertEquals(identifiers[0], applicationId);

        // release
        dccs.release(applicationId);

        // check registered clients
        identifiers = dccs.getCloudApplicationIdentifiers();
        assertEquals(0, identifiers.length);

        verify(esmMock, times(1)).shutdown(esMock);
    }

    @Test
    public void testNewClientCustomEndpointDispose() throws KuraException, NoSuchFieldException {
        CamelContext camelContextMock = mock(CamelContext.class);
        ExecutorServiceManager esmMock = mock(ExecutorServiceManager.class);
        ExecutorService esMock = mock(ExecutorService.class);
        when(esmMock.newThreadPool(anyObject(), anyString(), anyInt(), anyInt())).thenReturn(esMock);
        when(camelContextMock.getExecutorServiceManager()).thenReturn(esmMock);

        DefaultCamelCloudService dccs = new DefaultCamelCloudService(camelContextMock);

        String applicationId = "app";

        String endpoint = "endpoint";
        dccs.registerBaseEndpoint(applicationId, endpoint);

        CloudClient cloudClient = dccs.newCloudClient(applicationId);

        assertNotNull(cloudClient);
        assertTrue(cloudClient instanceof CamelCloudClient);
        assertEquals(cloudClient.getApplicationId(), applicationId);

        String endp = (String) TestUtil.getFieldValue(cloudClient, "baseEndpoint");
        assertEquals(endpoint, endp);

        // check registered clients
        String[] identifiers = dccs.getCloudApplicationIdentifiers();
        assertEquals(1, identifiers.length);
        assertEquals(identifiers[0], applicationId);

        // release all clients
        dccs.dispose();

        // check registered clients
        identifiers = dccs.getCloudApplicationIdentifiers();
        assertEquals(0, identifiers.length);

        verify(esmMock, times(2)).shutdown(esMock); // one more time due to the recursive call
    }

    @Test
    public void testNewClientDisposeError() throws KuraException, NoSuchFieldException {
        CamelContext camelContextMock = mock(CamelContext.class);
        ExecutorServiceManager esmMock = mock(ExecutorServiceManager.class);
        ExecutorService esMock = mock(ExecutorService.class);
        when(esmMock.newThreadPool(anyObject(), anyString(), anyInt(), anyInt())).thenReturn(esMock);
        doThrow(new RuntimeException("test")).when(esmMock).shutdown(esMock);
        when(camelContextMock.getExecutorServiceManager()).thenReturn(esmMock);

        DefaultCamelCloudService dccs = new DefaultCamelCloudService(camelContextMock);

        String applicationId = "app";

        CloudClient cloudClient = dccs.newCloudClient(applicationId);

        assertNotNull(cloudClient);
        assertTrue(cloudClient instanceof CamelCloudClient);
        assertEquals(cloudClient.getApplicationId(), applicationId);

        // check registered clients
        String[] identifiers = dccs.getCloudApplicationIdentifiers();
        assertEquals(1, identifiers.length);
        assertEquals(identifiers[0], applicationId);

        // release all clients with exception
        try {
            dccs.dispose();
            fail("Exception was expected.");
        } catch (RuntimeException e) {
            assertNotNull(e.getCause());
            assertEquals("test", e.getCause().getMessage());
        }

        // check registered clients
        identifiers = dccs.getCloudApplicationIdentifiers();
        assertEquals(0, identifiers.length);

        verify(esmMock, times(1)).shutdown(esMock); // one more time due to the recursive call
    }
}
