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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransportOptions;
import org.eclipse.kura.configuration.Password;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class SparkplugDataTransportOptionsTest {

    /*
     * Scenarios
     */

    @RunWith(Parameterized.class)
    public static class ServerUrisParameterFailTest extends Steps {

        @Parameters
        public static Collection<String> uris() {
            List<String> data = new LinkedList<>();
            data.add(null);
            data.add("");
            data.add("tcp://broker:1883/");
            data.add("tcp://broker:1883 tcp://broker:1883/");
            data.add("tcp://broker:1883  tcp://broker2:1883");
            return data;
        }

        private String serverUris;

        public ServerUrisParameterFailTest(String param) {
            this.serverUris = param;
        }

        @Test
        public void shouldThrowKuraExceptionOnWrongServerUri() {
            givenProperty(SparkplugDataTransportOptions.KEY_SERVER_URIS, this.serverUris);
            givenProperty(SparkplugDataTransportOptions.KEY_CLIENT_ID, "test");

            whenOptionsCreated();

            thenExceptionOccurred(KuraException.class);
        }
    }

    @RunWith(Parameterized.class)
    public static class ClientIdParameterFailTest extends Steps {

        @Parameters
        public static Collection<String> uris() {
            List<String> data = new LinkedList<>();
            data.add(null);
            data.add("");
            return data;
        }

        private String clientId;

        public ClientIdParameterFailTest(String param) {
            this.clientId = param;
        }

        @Test
        public void shouldThrowKuraExceptionOnWrongClientId() {
            givenProperty(SparkplugDataTransportOptions.KEY_SERVER_URIS, "tcp://broker:1883");
            givenProperty(SparkplugDataTransportOptions.KEY_CLIENT_ID, this.clientId);

            whenOptionsCreated();

            thenExceptionOccurred(KuraException.class);
        }
    }

    @RunWith(Parameterized.class)
    public static class ServerUrisParameterTest extends Steps {

        @Parameters
        public static Collection<String> uris() {
            List<String> data = new LinkedList<>();
            data.add("tcp://broker:1883");
            data.add("tcp://broker1:1883 tcp://broker2:1883");
            return data;
        }

        private String serverUris;

        public ServerUrisParameterTest(String param) {
            this.serverUris = param;
        }

        @Test
        public void shouldReturnCorrectPrimaryServerId() throws KuraException {
            givenProperty(SparkplugDataTransportOptions.KEY_SERVER_URIS, this.serverUris);
            givenProperty(SparkplugDataTransportOptions.KEY_CLIENT_ID, "test");
            givenOptionsCreated();

            whenGetPrimaryServerURI();

            thenReturnedStringEquals(this.serverUris.split(" ")[0]);
        }
    }

    public static class GettersTest extends Steps {

        @Test
        public void shouldReturnCorrectClientId() throws KuraException {
            givenProperty(SparkplugDataTransportOptions.KEY_SERVER_URIS, "tcp://broker:1883");
            givenProperty(SparkplugDataTransportOptions.KEY_CLIENT_ID, "test");
            givenOptionsCreated();

            whenGetClientId();

            thenReturnedStringEquals("test");
        }

        @Test
        public void shouldReturnCorrectUsername() throws KuraException {
            givenProperty(SparkplugDataTransportOptions.KEY_SERVER_URIS, "tcp://broker:1883");
            givenProperty(SparkplugDataTransportOptions.KEY_CLIENT_ID, "test");
            givenProperty(SparkplugDataTransportOptions.KEY_USERNAME, "user");
            givenOptionsCreated();

            whenGetUsername();

            thenReturnedStringEquals("user");
        }

        @Test
        public void shouldReturnCorrectConnectionTimeoutMs() throws KuraException {
            givenProperty(SparkplugDataTransportOptions.KEY_SERVER_URIS, "tcp://broker:1883");
            givenProperty(SparkplugDataTransportOptions.KEY_CLIENT_ID, "test");
            givenProperty(SparkplugDataTransportOptions.KEY_CONNECTION_TIMEOUT, 2);
            givenOptionsCreated();

            whenGetConnectionTimeoutMs();

            thenReturnedLongEquals(2000);
        }
    }

    public static class MqttConnectOptionsTest extends Steps {

        @Test
        public void shouldReturnCorrectPassword() throws KuraException {
            givenProperty(SparkplugDataTransportOptions.KEY_SERVER_URIS, "tcp://broker:1883");
            givenProperty(SparkplugDataTransportOptions.KEY_CLIENT_ID, "test");
            givenProperty(SparkplugDataTransportOptions.KEY_PASSWORD, new Password("secret"));
            givenOptionsCreated();

            whenGetMqttConnectOptions();

            thenMqttConnectOptionsContainsPassword("secret");
        }

        @Test
        public void shouldReturnCorrectKeepAlive() throws KuraException {
            givenProperty(SparkplugDataTransportOptions.KEY_SERVER_URIS, "tcp://broker:1883");
            givenProperty(SparkplugDataTransportOptions.KEY_CLIENT_ID, "test");
            givenProperty(SparkplugDataTransportOptions.KEY_KEEP_ALIVE, 12);
            givenOptionsCreated();

            whenGetMqttConnectOptions();

            thenMqttConnectOptionsContainsKeepAlive(12);
        }

    }

    /*
     * Steps
     */

    public abstract static class Steps {

        private SparkplugDataTransportOptions options;
        private Map<String, Object> properties = new HashMap<>();
        private Exception occurredException;
        private String returnedString;
        private long returnedLong;
        private MqttConnectOptions mqttConnectOptions;

        /*
         * Given
         */

        void givenProperty(String key, Object value) {
            this.properties.put(key, value);
        }

        void givenOptionsCreated() throws KuraException {
            this.options = new SparkplugDataTransportOptions(this.properties);
        }

        /*
         * When
         */

        void whenOptionsCreated() {
            try {
                givenOptionsCreated();
            } catch (Exception e) {
                this.occurredException = e;
            }
        }

        void whenGetPrimaryServerURI() {
            this.returnedString = this.options.getPrimaryServerURI();
        }

        void whenGetClientId() {
            this.returnedString = this.options.getClientId();
        }

        void whenGetUsername() {
            this.returnedString = this.options.getUsername();
        }

        void whenGetConnectionTimeoutMs() {
            this.returnedLong = this.options.getConnectionTimeoutMs();
        }

        void whenGetMqttConnectOptions() {
            this.mqttConnectOptions = this.options.getMqttConnectOptions();
        }

        /*
         * Then
         */

        <E extends Exception> void thenExceptionOccurred(Class<E> expectedException) {
            assertNotNull(this.occurredException);
            assertEquals(expectedException.getName(), this.occurredException.getClass().getName());
        }

        void thenReturnedStringEquals(String expectedResult) {
            assertEquals(expectedResult, this.returnedString);
        }

        void thenReturnedLongEquals(long expectedLong) {
            assertEquals(expectedLong, this.returnedLong);
        }

        void thenMqttConnectOptionsContainsPassword(String expectedPassword) {
            assertEquals(expectedPassword, new String(this.mqttConnectOptions.getPassword()));
        }

        void thenMqttConnectOptionsContainsKeepAlive(int expectedKeepAlive) {
            assertEquals(expectedKeepAlive, this.mqttConnectOptions.getKeepAliveInterval());
        }
    }

}
