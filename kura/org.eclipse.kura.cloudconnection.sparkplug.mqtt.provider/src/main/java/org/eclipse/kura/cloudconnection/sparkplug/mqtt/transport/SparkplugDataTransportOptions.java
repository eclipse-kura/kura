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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

public class SparkplugDataTransportOptions {

    public static final String KEY_GROUP_ID = "group.id";
    public static final String KEY_NODE_ID = "node.id";
    public static final String KEY_PRIMARY_HOST_APPLICATION_ID = "primary.host.application.id";
    public static final String KEY_SERVER_URIS = "server.uris";
    public static final String KEY_CLIENT_ID = "client.id";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_KEEP_ALIVE = "keep.alive";
    public static final String KEY_CONNECTION_TIMEOUT = "connection.timeout";

    private String groupId;
    private String nodeId;
    private Optional<String> primaryHostApplicationId = Optional.empty();
    private List<String> servers = new ArrayList<>();
    private MqttConnectOptions connectionOptions = new MqttConnectOptions();
    private String clientId;

    public SparkplugDataTransportOptions(Map<String, Object> properties) throws KuraException {
        this.groupId = (String) properties.getOrDefault(KEY_GROUP_ID, "group");
        this.nodeId = (String) properties.getOrDefault(KEY_NODE_ID, "node");
        this.primaryHostApplicationId = getOptionalString(properties.get(KEY_PRIMARY_HOST_APPLICATION_ID));

        setServers(getString(KEY_SERVER_URIS, properties).orElseThrow(getKuraException(KEY_SERVER_URIS)));
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

    public String getGroupId() {
        return this.groupId;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public Optional<String> getPrimaryHostApplicationId() {
        return this.primaryHostApplicationId;
    }

    public List<String> getServers() {
        return this.servers;
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

    private Optional<String> getOptionalString(Object value) {
        if (value instanceof String) {
            String result = ((String) value).trim();
            if (!result.isEmpty()) {
                return Optional.of(result);
            }
        }

        return Optional.empty();
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

    private void setServers(String spaceSeparatedservers) throws KuraException {
        String[] uris = spaceSeparatedservers.split(" ");
        for (String server : uris) {
            if (server.endsWith("/") || server.isEmpty()) {
                throw new KuraException(KuraErrorCode.INVALID_PARAMETER,
                        "Server URI cannot be empty, or end with '/', or contain a path");
            }

            this.servers.add(server);
        }
    }

}
