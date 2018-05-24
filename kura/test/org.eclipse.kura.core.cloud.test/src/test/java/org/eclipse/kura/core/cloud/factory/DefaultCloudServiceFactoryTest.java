/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.cloud.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.CloudConnectionManager;
import org.eclipse.kura.cloud.factory.CloudServiceFactory;
import org.eclipse.kura.configuration.ConfigurationService;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

public class DefaultCloudServiceFactoryTest {

    private static final String CLOUD_SERVICE_FACTORY_PID = "org.eclipse.kura.cloud.CloudService";

    @Test
    public void testCreateConfigurationWrongPid() {
        String pid = "test.service.pid";

        DefaultCloudServiceFactory factory = new DefaultCloudServiceFactory();

        try {
            factory.createConfiguration(pid);
            fail("Exception was expected");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.INVALID_PARAMETER, e.getCode());
        }
    }

    @Test
    public void testCreateConfiguration() throws KuraException {
        String pid = "org.eclipse.kura.cloud.CloudService-1";

        ConfigurationService csMock = mock(ConfigurationService.class);

        DefaultCloudServiceFactory factory = new DefaultCloudServiceFactory();
        factory.setConfigurationService(csMock);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();

                assertTrue(args[0].equals("org.eclipse.kura.cloud.CloudService")
                        || args[0].equals("org.eclipse.kura.data.DataService")
                        || args[0].equals("org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport"));

                if (args[0].equals("org.eclipse.kura.cloud.CloudService")) {
                    assertEquals(pid, args[1]);
                    assertFalse((boolean) args[3]);
                } else if (args[0].equals("org.eclipse.kura.data.DataService")) {
                    assertEquals("org.eclipse.kura.data.DataService-1", args[1]);
                    assertFalse((boolean) args[3]);
                } else if (args[0].equals("org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport")) {
                    assertEquals("org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport-1", args[1]);
                    assertTrue((boolean) args[3]);
                }

                return null;
            }
        }).when(csMock).createFactoryConfiguration(anyString(), anyString(), anyObject(), anyBoolean());

        factory.createConfiguration(pid);

        verify(csMock, times(3)).createFactoryConfiguration(anyString(), anyString(), anyObject(), anyBoolean());
    }

    @Test
    public void testDeleteConfigurationWrongPid() throws KuraException {
        String pid = "test.service.pid";

        ConfigurationService csMock = mock(ConfigurationService.class);

        DefaultCloudServiceFactory factory = new DefaultCloudServiceFactory();
        factory.setConfigurationService(csMock);

        factory.deleteConfiguration(pid);

        verify(csMock, times(0)).deleteFactoryConfiguration(anyString(), anyBoolean());
    }

    @Test
    public void testDeleteConfiguration() throws KuraException {
        String pid = "org.eclipse.kura.cloud.CloudService-1";

        ConfigurationService csMock = mock(ConfigurationService.class);

        DefaultCloudServiceFactory factory = new DefaultCloudServiceFactory();
        factory.setConfigurationService(csMock);

        factory.deleteConfiguration(pid);

        verify(csMock, times(1)).deleteFactoryConfiguration(pid, false);
        verify(csMock, times(1)).deleteFactoryConfiguration("org.eclipse.kura.data.DataService-1", false);
        verify(csMock, times(1))
                .deleteFactoryConfiguration("org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport-1", true);
    }

    @Test
    public void testGetStackComponentsPidsWrongPid() {
        String pid = "test.service.pid";

        DefaultCloudServiceFactory factory = new DefaultCloudServiceFactory();

        try {
            factory.getStackComponentsPids(pid);
            fail("Exception was expected");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.INVALID_PARAMETER, e.getCode());
        }
    }

    @Test
    public void testGetStackComponentsPids() throws KuraException {
        String pid = "org.eclipse.kura.cloud.CloudService-1";

        DefaultCloudServiceFactory factory = new DefaultCloudServiceFactory();

        List<String> stackComponentsPids = factory.getStackComponentsPids(pid);

        assertEquals(3, stackComponentsPids.size());
        assertEquals("org.eclipse.kura.cloud.CloudService-1", stackComponentsPids.get(0));
        assertEquals("org.eclipse.kura.data.DataService-1", stackComponentsPids.get(1));
        assertEquals("org.eclipse.kura.core.data.transport.mqtt.MqttDataTransport-1", stackComponentsPids.get(2));
    }

    @Test
    public void testGetManagedCloudServicePidsEmpty() throws KuraException {
        DefaultCloudServiceFactory factory = new DefaultCloudServiceFactory();

        factory.activate(getMockComponentContext(Collections.emptyList()));

        Set<String> pids = factory.getManagedCloudConnectionPids();

        assertNotNull(pids);
        assertEquals(0, pids.size());
    }

    private ComponentContext getMockComponentContext(Collection<ServiceReference<CloudConnectionManager>> refs) {
        try {
            final BundleContext context = mock(BundleContext.class);

            when(context.getServiceReferences(CloudConnectionManager.class, null)).thenReturn(refs);

            final ComponentContext componentContext = mock(ComponentContext.class);

            when(componentContext.getBundleContext()).thenReturn(context);

            return componentContext;
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException();
        }
    }

    private ServiceReference<CloudConnectionManager> createMockReference(final String pid, final String factoryPid,
            final Map<String, Object> properties) {

        final Map<String, Object> refProperties = new HashMap<>();

        if (properties != null) {
            refProperties.putAll(properties);
        }

        refProperties.put(ConfigurationService.KURA_SERVICE_PID, pid);
        refProperties.put(ConfigurationAdmin.SERVICE_FACTORYPID, factoryPid);

        final ServiceReference<CloudConnectionManager> ref = mock(ServiceReference.class);
        when(ref.getProperty(anyObject())).thenAnswer(answer -> {
            final String key = answer.getArgumentAt(0, String.class);
            return refProperties.get(key);
        });

        return ref;
    }

    @Test
    public void testGetManagedCloudServicePids() throws KuraException {
        DefaultCloudServiceFactory factory = new DefaultCloudServiceFactory();

        List<ServiceReference<CloudConnectionManager>> list = new ArrayList<>();
        list.add(createMockReference(CLOUD_SERVICE_FACTORY_PID + "-FOO", CLOUD_SERVICE_FACTORY_PID,
                Collections.singletonMap(CloudServiceFactory.KURA_CLOUD_SERVICE_FACTORY_PID,
                        DefaultCloudServiceFactory.class.getName())));

        list.add(createMockReference(CLOUD_SERVICE_FACTORY_PID, CLOUD_SERVICE_FACTORY_PID, Collections.singletonMap(
                CloudServiceFactory.KURA_CLOUD_SERVICE_FACTORY_PID, DefaultCloudServiceFactory.class.getName())));

        factory.activate(getMockComponentContext(list));

        Set<String> pids = factory.getManagedCloudConnectionPids();

        assertNotNull(pids);
        assertEquals(2, pids.size());
        assertTrue(pids.contains(CLOUD_SERVICE_FACTORY_PID));
        assertTrue(pids.contains(CLOUD_SERVICE_FACTORY_PID + "-FOO"));
    }

    @Test
    public void testGetManagedCloudServiceShouldOnlyReturnManagedEntries() throws KuraException {
        DefaultCloudServiceFactory factory = new DefaultCloudServiceFactory();

        List<ServiceReference<CloudConnectionManager>> list = new ArrayList<>();
        list.add(createMockReference(CLOUD_SERVICE_FACTORY_PID + "-OK", CLOUD_SERVICE_FACTORY_PID,
                Collections.singletonMap(CloudServiceFactory.KURA_CLOUD_SERVICE_FACTORY_PID,
                        DefaultCloudServiceFactory.class.getName())));

        list.add(createMockReference(CLOUD_SERVICE_FACTORY_PID + "-OK2", CLOUD_SERVICE_FACTORY_PID,
                Collections.singletonMap(CloudServiceFactory.KURA_CLOUD_SERVICE_FACTORY_PID,
                        DefaultCloudServiceFactory.class.getName())));

        list.add(createMockReference(CLOUD_SERVICE_FACTORY_PID + "-BAR", CLOUD_SERVICE_FACTORY_PID, Collections
                .singletonMap(CloudServiceFactory.KURA_CLOUD_SERVICE_FACTORY_PID, "OtherCloudServiceFactory")));

        list.add(createMockReference(CLOUD_SERVICE_FACTORY_PID, CLOUD_SERVICE_FACTORY_PID, Collections.singletonMap(
                CloudServiceFactory.KURA_CLOUD_SERVICE_FACTORY_PID, DefaultCloudServiceFactory.class.getName())));

        list.add(createMockReference("CloudServiceBaz", CLOUD_SERVICE_FACTORY_PID, Collections.emptyMap()));
        list.add(createMockReference("OtherCloudService", CLOUD_SERVICE_FACTORY_PID,
                Collections.singletonMap(CloudServiceFactory.KURA_CLOUD_SERVICE_FACTORY_PID, null)));

        list.add(createMockReference("foo", "bar", Collections.emptyMap()));

        factory.activate(getMockComponentContext(list));

        Set<String> pids = factory.getManagedCloudConnectionPids();

        assertNotNull(pids);
        assertEquals(3, pids.size());
        assertTrue(pids.contains(CLOUD_SERVICE_FACTORY_PID + "-OK"));
        assertTrue(pids.contains(CLOUD_SERVICE_FACTORY_PID + "-OK2"));
        assertTrue(pids.contains(CLOUD_SERVICE_FACTORY_PID));
    }
}
