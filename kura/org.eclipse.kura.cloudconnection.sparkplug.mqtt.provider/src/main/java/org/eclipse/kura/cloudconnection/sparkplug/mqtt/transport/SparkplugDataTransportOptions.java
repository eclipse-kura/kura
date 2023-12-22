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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

public class SparkplugDataTransportOptions {

    public static final String KEY_SERVER_URIS = "server.uris";
    public static final String KEY_CLIENT_ID = "client.id";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_KEEP_ALIVE = "keep.alive";
    public static final String KEY_CONNECTION_TIMEOUT = "connection.timeout";

    private MqttConnectOptions connectionOptions = new MqttConnectOptions();
    private String clientId;

    public SparkplugDataTransportOptions(Map<String, Object> properties) throws KuraException {
        String servers = getString(KEY_SERVER_URIS, properties).orElseThrow(getKuraException(KEY_SERVER_URIS));
        String clientIdentifier = getString(KEY_CLIENT_ID, properties).orElseThrow(getKuraException(KEY_CLIENT_ID));
        
        if (servers.isEmpty()) {
            throw getKuraException(KEY_SERVER_URIS).get();
        }

        if (clientIdentifier.isEmpty()) {
            throw getKuraException(KEY_CLIENT_ID).get();
        }

        Optional<String> username = getString(KEY_USERNAME, properties);
        Optional<Password> password = getPassword(KEY_PASSWORD, properties);
        Optional<Integer> connectionTimeout = getInteger(KEY_CONNECTION_TIMEOUT, properties);
        Optional<Integer> keepAlive = getInteger(KEY_KEEP_ALIVE, properties);

        this.connectionOptions.setServerURIs(getServerURIs(servers));
        this.clientId = clientIdentifier;

        if (username.isPresent()) {
            this.connectionOptions.setUserName(username.get());
        }

        if (password.isPresent()) {
            this.connectionOptions.setPassword(password.get().getPassword());
        }

        if (connectionTimeout.isPresent()) {
            this.connectionOptions.setConnectionTimeout(connectionTimeout.get());
        }

        if (keepAlive.isPresent()) {
            this.connectionOptions.setKeepAliveInterval(keepAlive.get());
        }

        this.connectionOptions.setCleanSession(true);
        this.connectionOptions.setAutomaticReconnect(false);
    }

    public MqttConnectOptions getMqttConnectOptions() {
        return this.connectionOptions;
    }

    public String getClientId() {
        return this.clientId;
    }

    public String getUsername() {
        return this.connectionOptions.getUserName();
    }

    public long getConnectionTimeoutMs() {
        return this.connectionOptions.getConnectionTimeout() * 1000L;
    }

    public String getPrimaryServerURI() {
        return this.connectionOptions.getServerURIs()[0];
    }

    private Optional<String> getString(String key, Map<String, Object> map) {
        return Optional.ofNullable((String) map.get(key));
    }

    private Optional<Password> getPassword(String key, Map<String, Object> map) {
        return Optional.ofNullable((Password) map.get(key));
    }

    private Optional<Integer> getInteger(String key, Map<String, Object> map) {
        return Optional.ofNullable((Integer) map.get(key));
    }

    private Supplier<KuraException> getKuraException(String propertyName) {
        return () -> new KuraException(KuraErrorCode.INVALID_PARAMETER,
                "The property " + propertyName + " is null or empty");
    }

    private String[] getServerURIs(String spaceSeparatedservers) throws KuraException {
        String[] servers = spaceSeparatedservers.split(" ");
        for (String server : servers) {
            if (server.endsWith("/") || server.isEmpty()) {
                throw new KuraException(KuraErrorCode.INVALID_PARAMETER,
                        "Server URI cannot be empty, or end with '/', or contain a path");
            }
        }
        return servers;
    }

}
