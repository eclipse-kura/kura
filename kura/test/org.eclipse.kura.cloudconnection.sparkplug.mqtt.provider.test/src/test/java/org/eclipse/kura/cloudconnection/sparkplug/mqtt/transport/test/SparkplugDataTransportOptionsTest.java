/*******************************************************************************
 * Copyright (c) 2023, 2024 Eurotech and/or its affiliates and others
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport.SparkplugDataTransportOptions;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.Before;
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
        public static Collection<String> params() {
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
    public static class ServerUrisParameterTest extends Steps {

        @Parameters
        public static Collection<String> params() {
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

            whenGetServers();

            thenReturnedServersContains(this.serverUris.split(" "));
        }
    }

    @RunWith(Parameterized.class)
    public static class MandatoryPropertiesTest extends Steps {

        @Parameters
        public static Collection<Object[]> params() {
            Collection<Object[]> data = new LinkedList<>();

            data.add(new Object[] { SparkplugDataTransportOptions.KEY_GROUP_ID, null });
            data.add(new Object[] { SparkplugDataTransportOptions.KEY_GROUP_ID, "" });
            data.add(new Object[] { SparkplugDataTransportOptions.KEY_NODE_ID, null });
            data.add(new Object[] { SparkplugDataTransportOptions.KEY_NODE_ID, "" });
            data.add(new Object[] { SparkplugDataTransportOptions.KEY_CLIENT_ID, null });
            data.add(new Object[] { SparkplugDataTransportOptions.KEY_CLIENT_ID, "" });
            data.add(new Object[] { SparkplugDataTransportOptions.KEY_KEEP_ALIVE, null });
            data.add(new Object[] { SparkplugDataTransportOptions.KEY_CONNECTION_TIMEOUT, null });

            return data;
        }

        private String testKey;
        private Object testValue;

        public MandatoryPropertiesTest(String key, Object value) {
            this.testKey = key;
            this.testValue = value;
        }

        @Test
        public void shouldThrowKuraExceptionOnNullMandatoryProperty() {
            givenProperty((String) this.testKey, this.testValue);

            whenOptionsCreated();

            thenExceptionOccurred(KuraException.class);
        }

    }

    public static class GettersTest extends Steps {

        @Test
        public void shouldReturnCorrectGroupId() throws KuraException {
            givenProperty(SparkplugDataTransportOptions.KEY_GROUP_ID, "g1");
            givenOptionsCreated();

            whenGetGroupId();

            thenReturnedStringEquals("g1");
        }

        @Test
        public void shouldReturnCorrectNodeId() throws KuraException {
            givenProperty(SparkplugDataTransportOptions.KEY_NODE_ID, "n1");
            givenOptionsCreated();

            whenGetNodeId();

            thenReturnedStringEquals("n1");
        }

        @Test
        public void shouldReturnCorrectPrimaryHostApplicationId() throws KuraException {
            givenProperty(SparkplugDataTransportOptions.KEY_PRIMARY_HOST_APPLICATION_ID, "h1");
            givenOptionsCreated();

            whenGetPrimaryHostApplicationId();

            thenRetunedPrimaryHostApplicationIs("h1");
        }

        @Test
        public void shouldReturnEmptyPrimaryHostApplicationId() throws KuraException {
            givenOptionsCreated();

            whenGetPrimaryHostApplicationId();

            thenRetunedPrimaryHostApplicationIdIsEmpty();
        }

        @Test
        public void shouldReturnCorrectClientId() throws KuraException {
            givenProperty(SparkplugDataTransportOptions.KEY_CLIENT_ID, "test.client");
            givenOptionsCreated();

            whenGetClientId();

            thenReturnedStringEquals("test.client");
        }

        @Test
        public void shouldReturnCorrectUsername() throws KuraException {
            givenProperty(SparkplugDataTransportOptions.KEY_USERNAME, "user");
            givenOptionsCreated();

            whenGetUsername();

            thenReturnedStringEquals("user");
        }

        @Test
        public void shouldReturnCorrectConnectionTimeoutMs() throws KuraException {
            givenProperty(SparkplugDataTransportOptions.KEY_CONNECTION_TIMEOUT, 30);
            givenOptionsCreated();

            whenGetConnectionTimeoutMs();

            thenReturnedLongEquals(30000L);
        }
    }

    public static class MqttConnectOptionsTest extends Steps {

        @Test
        public void shouldReturnCorrectPassword() throws KuraException {
            givenProperty(SparkplugDataTransportOptions.KEY_PASSWORD, "secret");
            givenOptionsCreated();

            whenGetMqttConnectOptions();

            thenMqttConnectOptionsContainsPassword("secret");
        }

        @Test
        public void shouldNotSetPasswordWhenEmpty() throws KuraException {
            givenProperty(SparkplugDataTransportOptions.KEY_PASSWORD, "");
            givenOptionsCreated();

            whenGetMqttConnectOptions();

            thenMqttConnectOptionsNotSetsPassword();
        }

        @Test
        public void shouldNotSetUsernameAndPassword() throws KuraException {
            givenOptionsCreated();

            whenGetMqttConnectOptions();

            thenMqttConnectOptionsNotSetsUsername();
            thenMqttConnectOptionsNotSetsPassword();
        }

        @Test
        public void shouldReturnCorrectKeepAlive() throws KuraException {
            givenProperty(SparkplugDataTransportOptions.KEY_KEEP_ALIVE, 12);
            givenOptionsCreated();

            whenGetMqttConnectOptions();

            thenMqttConnectOptionsContainsKeepAlive(12);
        }

        @Test
        public void shouldReturnCorrectConnectionTimeout() throws KuraException {
            givenProperty(SparkplugDataTransportOptions.KEY_CONNECTION_TIMEOUT, 20);
            givenOptionsCreated();

            whenGetMqttConnectOptions();

            thenMqttConnectOptionsContainsConnectionTimeout(20);
        }

        @Test
        public void shouldSetCleanSessionTrue() throws KuraException {
            givenOptionsCreated();

            whenGetMqttConnectOptions();

            thenMqttConnectOptionsHasCleanSessionEnabled();
        }

        @Test
        public void shouldSetAutoReconnectFalse() throws KuraException {
            givenOptionsCreated();

            whenGetMqttConnectOptions();

            thenMqttConnectOptionsHasAutoReconnectDisabled();
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
        private List<String> returnedServers = new LinkedList<>();
        private Optional<String> returnedPrimaryHostApplicationId = Optional.empty();
        private long returnedLong;
        private MqttConnectOptions mqttConnectOptions;
        private CryptoService cryptoServiceMock;

        @Before
        public void defaults() throws KuraException {
            givenProperty(SparkplugDataTransportOptions.KEY_GROUP_ID, "g1");
            givenProperty(SparkplugDataTransportOptions.KEY_NODE_ID, "n1");
            givenProperty(SparkplugDataTransportOptions.KEY_SERVER_URIS, "tcp://broker:1883");
            givenProperty(SparkplugDataTransportOptions.KEY_CLIENT_ID, "test.client");
            givenProperty(SparkplugDataTransportOptions.KEY_KEEP_ALIVE, 60);
            givenProperty(SparkplugDataTransportOptions.KEY_CONNECTION_TIMEOUT, 30);
            this.cryptoServiceMock = mock(CryptoService.class);
            doAnswer(invocation -> (char[]) invocation.getArgument(0)).when(cryptoServiceMock)
                    .decryptAes(any(char[].class));
        }

        /*
         * Given
         */

        void givenProperty(String key, Object value) {
            this.properties.put(key, value);
        }

        void givenOptionsCreated() throws KuraException {
            this.options = new SparkplugDataTransportOptions(this.properties, this.cryptoServiceMock);
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

        void whenGetGroupId() {
            this.returnedString = this.options.getGroupId();
        }

        void whenGetNodeId() {
            this.returnedString = this.options.getNodeId();
        }

        void whenGetPrimaryHostApplicationId() {
            this.returnedPrimaryHostApplicationId = this.options.getPrimaryHostApplicationId();
        }

        void whenGetServers() {
            this.returnedServers = this.options.getServers();
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

        void thenReturnedServersContains(String[] expectedServers) {
            for (String expectedServer : expectedServers) {
                assertTrue(this.returnedServers.contains(expectedServer));
            }
        }

        void thenRetunedPrimaryHostApplicationIdIsEmpty() {
            assertFalse(this.returnedPrimaryHostApplicationId.isPresent());
        }

        void thenRetunedPrimaryHostApplicationIs(String expectedPrimaryHostApplicationId) {
            assertTrue(this.returnedPrimaryHostApplicationId.isPresent());
            assertEquals(expectedPrimaryHostApplicationId, this.returnedPrimaryHostApplicationId.get());
        }

        void thenMqttConnectOptionsContainsPassword(String expectedPassword) {
            assertEquals(expectedPassword, new String(this.mqttConnectOptions.getPassword()));
        }

        void thenMqttConnectOptionsContainsKeepAlive(int expectedKeepAlive) {
            assertEquals(expectedKeepAlive, this.mqttConnectOptions.getKeepAliveInterval());
        }

        void thenMqttConnectOptionsNotSetsUsername() {
            assertNull(this.mqttConnectOptions.getUserName());
        }

        void thenMqttConnectOptionsNotSetsPassword() {
            assertNull(this.mqttConnectOptions.getPassword());
        }

        void thenMqttConnectOptionsHasCleanSessionEnabled() {
            assertTrue(this.mqttConnectOptions.isCleanSession());
        }

        void thenMqttConnectOptionsHasAutoReconnectDisabled() {
            assertFalse(this.mqttConnectOptions.isAutomaticReconnect());
        }

        void thenMqttConnectOptionsContainsConnectionTimeout(int expectedConnectionTimeout) {
            assertEquals(expectedConnectionTimeout, this.mqttConnectOptions.getConnectionTimeout());
        }
    }

}
