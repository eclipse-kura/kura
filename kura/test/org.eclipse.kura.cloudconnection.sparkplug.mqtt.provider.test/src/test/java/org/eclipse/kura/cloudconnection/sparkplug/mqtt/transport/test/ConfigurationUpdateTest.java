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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransport;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransportOptions;
import org.eclipse.kura.data.transport.listener.DataTransportListener;
import org.eclipse.kura.ssl.SslManagerService;
import org.junit.Before;
import org.junit.Test;

public class ConfigurationUpdateTest {

    private SparkplugDataTransport transport = new SparkplugDataTransport();;
    private DataTransportListener listener = mock(DataTransportListener.class);
    private SslManagerService sslManagerService = mock(SslManagerService.class);
    private Exception occurredException;

    /*
     * Scenarios
     */

    @Test
    public void test() {
        assertTrue(true);
    }

    @Test
    public void shouldUpdateConfigurationOnSetSslManager() {
        givenUpdated();

        whenSetSslManagerService();

        thenUpdateWasCalled(2);
    }

    @Test
    public void shouldNotUpdateConfigurationOnNewSslManagerButNullOptions() {
        whenSetSslManagerService();

        thenUpdateWasCalled(0);
    }

    @Test
    public void shouldUpdateConfigurationOnUnsetSslManager() {
        givenSetSslManagerService();
        givenUpdated();

        whenUnsetSslManagerService();

        thenUpdateWasCalled(2);
    }

    @Test
    public void shouldNotThrowExceptionsOnDisconnectWithUnconfiguredService() {
        whenDisconnect();

        thenNoExceptionOccurred();
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenUpdated() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(SparkplugDataTransportOptions.KEY_CLIENT_ID, "test.client");
        properties.put(SparkplugDataTransportOptions.KEY_CONNECTION_TIMEOUT, 30);
        properties.put(SparkplugDataTransportOptions.KEY_GROUP_ID, "g1");
        properties.put(SparkplugDataTransportOptions.KEY_NODE_ID, "n1");
        properties.put(SparkplugDataTransportOptions.KEY_PRIMARY_HOST_APPLICATION_ID, "");
        properties.put(SparkplugDataTransportOptions.KEY_SERVER_URIS, "tcp://localhost:1883");
        properties.put(SparkplugDataTransportOptions.KEY_KEEP_ALIVE, 60);

        this.transport.update(properties);
    }

    private void givenSetSslManagerService() {
        this.transport.setSslManagerService(this.sslManagerService);
    }

    /*
     * When
     */

    private void whenSetSslManagerService() {
        givenSetSslManagerService();
    }

    private void whenUnsetSslManagerService() {
        this.transport.unsetSslManagerService(this.sslManagerService);
    }

    private void whenDisconnect() {
        try {
            this.transport.disconnect(0);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    /*
     * Then
     */

    private void thenUpdateWasCalled(int expectedTimes) {
        verify(this.listener, times(expectedTimes)).onConfigurationUpdated(anyBoolean());
    }

    private void thenNoExceptionOccurred() {
        assertNull("Exception has occurred", this.occurredException);
    }

    /*
     * Utils
     */

    @Before
    public void setup() throws GeneralSecurityException, IOException {
        this.transport.addDataTransportListener(this.listener);
        SSLContext context = mock(SSLContext.class);
        when(this.sslManagerService.getSSLContext()).thenReturn(context);
    }

}
