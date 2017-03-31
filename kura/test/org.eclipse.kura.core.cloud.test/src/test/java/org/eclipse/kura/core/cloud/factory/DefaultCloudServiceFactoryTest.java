/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Icon;
import org.eclipse.kura.configuration.metatype.OCD;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class DefaultCloudServiceFactoryTest {

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

        ConfigurationService csMock = mock(ConfigurationService.class);
        factory.setConfigurationService(csMock);

        List<ComponentConfiguration> list = new ArrayList<ComponentConfiguration>();
        when(csMock.getComponentConfigurations()).thenReturn(list);

        Set<String> pids = factory.getManagedCloudServicePids();

        assertNotNull(pids);
        assertEquals(0, pids.size());
    }

    @Test
    public void testGetManagedCloudServicePids() throws KuraException {
        DefaultCloudServiceFactory factory = new DefaultCloudServiceFactory();

        ConfigurationService csMock = mock(ConfigurationService.class);
        factory.setConfigurationService(csMock);

        List<ComponentConfiguration> list = new ArrayList<ComponentConfiguration>();
        OCD ocd = new OCDImpl("id");
        ComponentConfiguration cc = new CCImpl("pid", ocd);
        list.add(cc);
        ocd = new OCDImpl("org.eclipse.kura.cloud.CloudService");
        cc = new CCImpl("pid2", ocd);
        list.add(cc);
        when(csMock.getComponentConfigurations()).thenReturn(list);

        Set<String> pids = factory.getManagedCloudServicePids();

        assertNotNull(pids);
        assertEquals(1, pids.size());
        assertEquals("pid2", pids.iterator().next());
    }
}

class CCImpl implements ComponentConfiguration {

    private String pid;
    private OCD definition;
    private Map<String, Object> properties = new HashMap<String, Object>();

    public CCImpl(String pid, OCD definition) {
        this.pid = pid;
        this.definition = definition;
    }

    @Override
    public String getPid() {
        return pid;
    }

    @Override
    public OCD getDefinition() {
        return definition;
    }

    @Override
    public Map<String, Object> getConfigurationProperties() {
        return properties;
    }
}

class OCDImpl implements OCD {

    private String id;

    public OCDImpl(String id) {
        this.id = id;
    }

    @Override
    public List<AD> getAD() {
        return null;
    }

    @Override
    public List<Icon> getIcon() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getId() {
        return id;
    }

}
