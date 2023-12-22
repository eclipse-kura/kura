/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.factory.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.factory.SparkplugCloudConnectionFactory;
import org.eclipse.kura.configuration.ConfigurationService;
import org.junit.Before;
import org.junit.Test;

public class SparkplugCloudConnectionFactoryTest {

    private SparkplugCloudConnectionFactory factory = new SparkplugCloudConnectionFactory();
    private ConfigurationService configuratioServiceMock = mock(ConfigurationService.class);
    private String returnedFactoryPid;
    private Exception occurredException;
    private List<String> returnedStackComponentPids;

    /*
     * Scenarios
     */

    @Test
    public void shouldReturnCorrectFactoryPid() {
        whenGetFactoryPid();
        thenReturnedFactoryPidEquals("org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint");
    }

    @Test
    public void shouldFailWithOtherComponentPid() {
        whenCreateConfiguration("org.eclipse.kura.cloudconnection.nonsparkplug.mqtt.endpoint.Example");

        thenExceptionOccurred(KuraException.class);
    }

    @Test
    public void shouldCreateCloudStackComponentsWithSuffix() throws KuraException {
        whenCreateConfiguration("org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint-test");

        thenFactoryComponentIsCreated(
                "org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint",
                "org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint-test",
                new ExpectedPropertiesBuilder()
                    .withProperty("DataService.target",
                        "(kura.service.pid=org.eclipse.kura.cloudconnection.sparkplug.mqtt.data.SparkplugDataService-test)")
                    .withProperty(CloudConnectionFactory.KURA_CLOUD_CONNECTION_FACTORY_PID,
                        "org.eclipse.kura.cloudconnection.sparkplug.mqtt.factory.SparkplugCloudConnectionFactory")
                    .withProperty("DataTransportService.target",
                        "(kura.service.pid=org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransport-test)")
                    .build(),
                false);
        thenFactoryComponentIsCreated(
                "org.eclipse.kura.data.DataService",
                "org.eclipse.kura.cloudconnection.sparkplug.mqtt.data.SparkplugDataService-test",
                new ExpectedPropertiesBuilder()
                    .withProperty("DataTransportService.target",
                        "(kura.service.pid=org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransport-test)")
                    .build(),
                false);
        thenFactoryComponentIsCreated(
                "org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransport",
                "org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransport-test",
                null,
                true);
    }
    
    @Test
    public void shouldCreateCloudStackComponentsWithoutSuffix() throws KuraException {
        whenCreateConfiguration("org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint");

        thenFactoryComponentIsCreated(
                "org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint",
                "org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint",
                new ExpectedPropertiesBuilder()
                    .withProperty("DataService.target",
                        "(kura.service.pid=org.eclipse.kura.cloudconnection.sparkplug.mqtt.data.SparkplugDataService)")
                    .withProperty(CloudConnectionFactory.KURA_CLOUD_CONNECTION_FACTORY_PID,
                        "org.eclipse.kura.cloudconnection.sparkplug.mqtt.factory.SparkplugCloudConnectionFactory")
                    .withProperty("DataTransportService.target",
                            "(kura.service.pid=org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransport)")
                    .build(),
                false);
        thenFactoryComponentIsCreated(
                "org.eclipse.kura.data.DataService",
                "org.eclipse.kura.cloudconnection.sparkplug.mqtt.data.SparkplugDataService",
                new ExpectedPropertiesBuilder()
                    .withProperty("DataTransportService.target",
                        "(kura.service.pid=org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransport)")
                    .build(),
                false);
        thenFactoryComponentIsCreated(
                "org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransport",
                "org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransport",
                null,
                true);
    }

    @Test
    public void shouldReturnCorrectStackComponentPidsWithSuffix() throws KuraException {
        whenGetStackComponentsPids(
                "org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint-test");

        thenReturnedStackComponentPidsContain(
                "org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint-test");
        thenReturnedStackComponentPidsContain(
                "org.eclipse.kura.cloudconnection.sparkplug.mqtt.data.SparkplugDataService-test");
        thenReturnedStackComponentPidsContain(
                "org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransport-test");
    }

    @Test
    public void shouldReturnCorrectStackComponentPidsWithoutSuffix() throws KuraException {
        whenGetStackComponentsPids("org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint");

        thenReturnedStackComponentPidsContain(
                "org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint");
        thenReturnedStackComponentPidsContain(
                "org.eclipse.kura.cloudconnection.sparkplug.mqtt.data.SparkplugDataService");
        thenReturnedStackComponentPidsContain(
                "org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransport");
    }

    @Test
    public void shouldDeleteCloudStackComponentsWithSuffix() throws KuraException {
        whenDeleteConfiguration("org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint-test");

        thenFactoryComponentIsDeleted(
                "org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint.SparkplugCloudEndpoint-test", false);
        thenFactoryComponentIsDeleted("org.eclipse.kura.cloudconnection.sparkplug.mqtt.data.SparkplugDataService-test",
                false);
        thenFactoryComponentIsDeleted(
                "org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransport-test", true);
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    /*
     * When
     */

    private void whenGetFactoryPid() {
        this.returnedFactoryPid = this.factory.getFactoryPid();
    }

    private void whenCreateConfiguration(String userPid) {
        try {
            this.factory.createConfiguration(userPid);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenGetStackComponentsPids(String userPid) throws KuraException {
        this.returnedStackComponentPids = this.factory.getStackComponentsPids(userPid);
    }

    private void whenDeleteConfiguration(String userPid) throws KuraException {
        this.factory.deleteConfiguration(userPid);
    }

    /*
     * Then
     */

    private void thenReturnedFactoryPidEquals(String expectedFactoryPid) {
        assertEquals(expectedFactoryPid, this.returnedFactoryPid);
    }

    private <E extends Exception> void thenExceptionOccurred(Class<E> expectedException) {
        assertNotNull(this.occurredException);
        assertEquals(expectedException.getName(), this.occurredException.getClass().getName());
    }

    private void thenFactoryComponentIsCreated(String expectedFactoryPid, String expectedPid,
            Map<String, Object> expectedProperties, boolean expectedTakeSnapshot) throws KuraException {
        verify(this.configuratioServiceMock, times(1)).createFactoryConfiguration(expectedFactoryPid, expectedPid,
                expectedProperties, expectedTakeSnapshot);
    }

    private void thenReturnedStackComponentPidsContain(String expectedPid) {
        assertTrue(this.returnedStackComponentPids.contains(expectedPid));
    }

    private void thenFactoryComponentIsDeleted(String expectedPid, boolean expectedTakeSnapshot) throws KuraException {
        verify(this.configuratioServiceMock, times(1)).deleteFactoryConfiguration(expectedPid, expectedTakeSnapshot);
    }

    /*
     * Utilities
     */

    @Before
    public void setup() {
        this.factory.setConfigurationService(this.configuratioServiceMock);
    }

    private class ExpectedPropertiesBuilder {
        
        private Map<String, Object> properties = new HashMap<>();

        public ExpectedPropertiesBuilder withProperty(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }
        
        public Map<String, Object> build() {
            return this.properties;
        }
        
    }

}
