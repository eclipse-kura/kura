/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.request.RequestHandlerContext;
import org.eclipse.kura.cloudconnection.request.RequestHandlerMessageConstants;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.internal.xml.marshaller.unmarshaller.XmlMarshallUnmarshallImpl;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.system.SystemService;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationAdmin;

public class CloudConfigurationHandlerTest {

    private CloudConfigurationHandler cloudConfigurationHandler;

    private ConfigurationService mockConfigurationService = mock(ConfigurationService.class);
    private RequestHandlerRegistry mockRequestHandlerRegistry = mock(RequestHandlerRegistry.class);
    private SystemService mockSystemService = mock(SystemService.class);
    private XmlMarshallUnmarshallImpl xmlMarshallerUnmarshall = new XmlMarshallUnmarshallImpl();

    private Exception occurredException;

    private KuraMessage requestMessage;

    private Set<String> pids = new HashSet<>();

    private String xmlComponentConfigurationTemplate = //
            "<esf:configurations xmlns:esf=\"http://eurotech.com/esf/2.0\" xmlns:ocd=\"http://www.osgi.org/xmlns/metatype/v1.2.0\">"
                    + "<esf:configuration pid=\"%s\">%s</esf:configuration>" + //
                    "</esf:configurations>";

    private String xmlComponentConfigurationStringElementTemplate = //
            "<esf:property array=\"false\" encrypted=\"false\" name=\"%s\" type=\"String\">"
                    + "<esf:value>%s</esf:value>" + //
                    "</esf:property>";

    @Test
    public void shouldUpdateComponentWithExistingPid() {

        givenCloudConfigurationHandler();
        givenExistingComponentWithPid("testPid", Collections.emptyMap());
        givenComponentToUpdate("testPid", toPropertyMap("testKey", "testValue"));

        whenUpdate();

        thenNoExceptionOccurred();
    }

    @Test
    public void shouldUpdateComponentWithFactoryPidAndNonExistingPid() {

        givenCloudConfigurationHandler();
        givenComponentToUpdate("testPid",
                toPropertyMap("testKey", "testValue", ConfigurationAdmin.SERVICE_FACTORYPID, "testPid"));

        whenUpdate();

        thenNoExceptionOccurred();
    }

    @Test
    public void shouldNotUpdateComponentWithNonExistingPid() {

        givenCloudConfigurationHandler();
        givenComponentToUpdate("testPid", toPropertyMap("testKey", "testValue"));

        whenUpdate();

        thenExceptionOccurred(KuraException.class);
    }

    private void givenCloudConfigurationHandler() {
        this.cloudConfigurationHandler = new CloudConfigurationHandler() {

            @Override
            protected String marshal(Object object) {
                try {
                    return CloudConfigurationHandlerTest.this.xmlMarshallerUnmarshall.marshal(object);
                } catch (KuraException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected <T> T unmarshal(String xmlString, Class<T> clazz) throws KuraException {
                return CloudConfigurationHandlerTest.this.xmlMarshallerUnmarshall.unmarshal(xmlString, clazz);
            }
        };

        this.cloudConfigurationHandler.setConfigurationService(this.mockConfigurationService);
        this.cloudConfigurationHandler.setRequestHandlerRegistry(this.mockRequestHandlerRegistry);
        this.cloudConfigurationHandler.setSystemService(this.mockSystemService);

    }

    private void givenExistingComponentWithPid(String pid, Map<String, Object> properties) {
        try {
            when(this.mockConfigurationService.getComponentConfiguration(pid))
                    .thenReturn(new ComponentConfigurationImpl(pid, null, properties));

            this.pids.add(pid);
            when(this.mockConfigurationService.getConfigurableComponentPids()).thenReturn(this.pids);
        } catch (KuraException e) {
            e.printStackTrace();
            fail("Unable to set component configuration");
        }
    }

    private void givenComponentToUpdate(String pid, Map<String, Object> properties) {
        KuraPayload payload = new KuraPayload();
        String propertiesString = "";
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            propertiesString += String.format(this.xmlComponentConfigurationStringElementTemplate, entry.getKey(),
                    entry.getValue());
        }
        payload.setBody(String.format(this.xmlComponentConfigurationTemplate, pid,
                "<esf:properties>" + propertiesString + "</esf:properties>").getBytes(StandardCharsets.UTF_8));

        List<String> resources = new ArrayList<>();
        resources.add(CloudConfigurationHandler.RESOURCE_CONFIGURATIONS);

        properties.put(RequestHandlerMessageConstants.ARGS_KEY.value(), resources);
        this.requestMessage = new KuraMessage(payload, properties);

    }

    private void whenUpdate() {
        try {
            this.cloudConfigurationHandler.doPut(mock(RequestHandlerContext.class), this.requestMessage);
        } catch (KuraException e) {
            this.occurredException = e;
        }
    }

    private Map<String, Object> toPropertyMap(String... properties) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < properties.length; i += 2) {
            map.put(properties[i], properties[i + 1]);
        }

        return map;
    }

    private void thenNoExceptionOccurred() {
        String errorMessage = "Empty message";
        if (Objects.nonNull(this.occurredException)) {
            StringWriter sw = new StringWriter();
            this.occurredException.printStackTrace(new PrintWriter(sw));

            errorMessage = String.format("No exception expected, \"%s\" found. Caused by: %s",
                    this.occurredException.getClass().getName(), sw.toString());
        }

        assertNull(errorMessage, this.occurredException);
    }

    private <E extends Exception> void thenExceptionOccurred(Class<E> expectedException) {
        assertNotNull(this.occurredException);
        assertEquals(expectedException.getName(), this.occurredException.getClass().getName());
    }
}
