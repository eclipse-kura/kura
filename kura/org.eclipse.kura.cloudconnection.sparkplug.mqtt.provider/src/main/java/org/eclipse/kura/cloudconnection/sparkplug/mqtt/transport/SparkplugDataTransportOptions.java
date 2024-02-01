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
import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.crypto.CryptoService;
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

    private final String groupId;
    private final String nodeId;
    private final Optional<String> primaryHostApplicationId;
    private final List<String> servers;
    private final MqttConnectOptions connectionOptions = new MqttConnectOptions();
    private final String clientId;

    public SparkplugDataTransportOptions(final Map<String, Object> properties, final CryptoService cryptoService)
            throws KuraException {        
        this.groupId = getMandatoryString(KEY_GROUP_ID, properties);
        this.nodeId = getMandatoryString(KEY_NODE_ID, properties);
        this.primaryHostApplicationId = getOptionalString(KEY_PRIMARY_HOST_APPLICATION_ID, properties);
        this.servers = getServersList(KEY_SERVER_URIS, properties);
        this.clientId = getMandatoryString(KEY_CLIENT_ID, properties);

        Optional<String> username = getOptionalString(KEY_USERNAME, properties);
        Optional<Password> password = getOptionalPassword(KEY_PASSWORD, properties);
        if (username.isPresent()) {
            this.connectionOptions.setUserName(username.get());
        }
        if (password.isPresent()) {
            this.connectionOptions.setPassword(cryptoService.decryptAes(password.get().getPassword()));
        }

        this.connectionOptions.setKeepAliveInterval(getMandatoryInt(KEY_KEEP_ALIVE, properties));
        this.connectionOptions.setConnectionTimeout(getMandatoryInt(KEY_CONNECTION_TIMEOUT, properties));

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

    private String getMandatoryString(String key, Map<String, Object> map) throws KuraException {
        String value = (String) map.get(key);

        if (Objects.isNull(value) || value.isEmpty()) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, key + " cannot be empty or null");
        }

        return value;
    }

    private int getMandatoryInt(String key, Map<String, Object> map) throws KuraException {
        Integer value = (Integer) map.get(key);

        if (Objects.isNull(value)) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, key + " cannot be null");
        }
        
        return value;
    }

    private Optional<String> getOptionalString(String key, Map<String, Object> map) {
        String value = (String) map.get(key);

        if (Objects.isNull(value) || value.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(value);
        }
    }

    private Optional<Password> getOptionalPassword(String key, Map<String, Object> map) {
        String value = (String) map.get(key);

        if (Objects.isNull(value) || value.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(new Password(value));
        }
    }

    private List<String> getServersList(String key, Map<String, Object> map) throws KuraException {
        String spaceSeparatedList = (String) map.get(key);

        if (Objects.isNull(spaceSeparatedList) || spaceSeparatedList.isEmpty()) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER, key + " cannot be empty or null");
        }

        List<String> result = new ArrayList<>();

        String[] uris = spaceSeparatedList.split(" ");
        for (String server : uris) {
            if (server.endsWith("/") || server.isEmpty()) {
                throw new KuraException(KuraErrorCode.INVALID_PARAMETER,
                        key + " items cannot be empty, or end with '/', or contain a path");
            }

            result.add(server);
        }

        return result;
    }

}
