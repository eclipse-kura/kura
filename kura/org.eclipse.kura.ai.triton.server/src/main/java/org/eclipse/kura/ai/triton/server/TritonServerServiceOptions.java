/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.ai.triton.server;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;

public class TritonServerServiceOptions {

    private static final String PROPERTY_ADDRESS = "server.address";
    private static final String PROPERTY_PORTS = "server.ports";
    private static final String PROPERTY_LOCAL_MODEL_REPOSITORY_PATH = "local.model.repository.path";
    private static final String PROPERTY_LOCAL_MODEL_REPOSITORY_PASSWORD = "local.model.repository.password";
    private static final String PROPERTY_LOCAL_BACKENDS_PATH = "local.backends.path";
    private static final String PROPERTY_LOCAL_BACKENDS_CONFIG = "local.backends.config";
    private static final String PROPERTY_MODELS = "models";
    private static final String PROPERTY_LOCAL = "enable.local";
    private final Map<String, Object> properties;

    private final int httpPort;
    private final int grpcPort;
    private final int metricsPort;
    private final boolean isLocal;

    public TritonServerServiceOptions(final Map<String, Object> properties) {
        requireNonNull(properties, "Properties cannot be null");
        this.properties = Collections.unmodifiableMap(new HashMap<>(properties));

        final Object propertyPorts = this.properties.get(PROPERTY_PORTS);
        if (propertyPorts instanceof Integer[]) {
            Integer[] ports = (Integer[]) propertyPorts;

            if (ports.length < 3) {
                throw new KuraRuntimeException(KuraErrorCode.INVALID_PARAMETER, PROPERTY_PORTS);
            }

            for (Integer port : ports) {
                requireNonNull(port, "port cannot be null");
            }

            this.httpPort = ports[0];
            this.grpcPort = ports[1];
            this.metricsPort = ports[2];
        } else {
            this.httpPort = 5000;
            this.grpcPort = 5001;
            this.metricsPort = 5002;
        }

        final Object propertyLocal = this.properties.get(PROPERTY_LOCAL);
        if (propertyLocal instanceof Boolean) {
            this.isLocal = (Boolean) propertyLocal;
        } else {
            this.isLocal = false;
        }
    }

    public String getAddress() {
        String address = "";
        if (this.isLocal) {
            address = "localhost";
        } else {
            final Object propertyAddress = this.properties.get(PROPERTY_ADDRESS);
            if (propertyAddress instanceof String) {
                address = ((String) propertyAddress).trim();
            }
        }
        return address;
    }

    public boolean isLocalEnabled() {
        return this.isLocal;
    }

    public int getHttpPort() {
        return this.httpPort;
    }

    public int getGrpcPort() {
        return this.grpcPort;
    }

    public int getMetricsPort() {
        return this.metricsPort;
    }

    public String getModelRepositoryPath() {
        return getStringProperty(PROPERTY_LOCAL_MODEL_REPOSITORY_PATH);
    }

    public String getModelRepositoryPassword() {
        return getStringProperty(PROPERTY_LOCAL_MODEL_REPOSITORY_PASSWORD);
    }

    public String getBackendsPath() {
        return getStringProperty(PROPERTY_LOCAL_BACKENDS_PATH);
    }

    public boolean modelsAreEncrypted() {
        return !getModelRepositoryPassword().isEmpty();
    }

    public List<String> getBackendsConfigs() {
        List<String> backendsConfigs = new ArrayList<>();
        final Object propertyBackendsConfig = this.properties.get(PROPERTY_LOCAL_BACKENDS_CONFIG);
        if (propertyBackendsConfig instanceof String && !((String) propertyBackendsConfig).isEmpty()) {
            backendsConfigs = Arrays.asList(((String) propertyBackendsConfig).trim().split(";"));
        }
        return backendsConfigs;
    }

    public List<String> getModels() {
        List<String> models = new ArrayList<>();
        final Object propertyModels = this.properties.get(PROPERTY_MODELS);
        if (propertyModels instanceof String && !((String) propertyModels).isEmpty()) {
            models = Arrays.asList(((String) propertyModels).replace(" ", "").split(","));
        }
        return models;
    }

    private String getStringProperty(String propertyName) {
        String stringProperty = "";
        final Object stringPropertyObj = this.properties.get(propertyName);
        if (stringPropertyObj instanceof String) {
            stringProperty = (String) stringPropertyObj;
        }
        return stringProperty;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.grpcPort, this.httpPort, this.isLocal, this.metricsPort, this.properties);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TritonServerServiceOptions)) {
            return false;
        }
        TritonServerServiceOptions other = (TritonServerServiceOptions) obj;
        return this.grpcPort == other.grpcPort && this.httpPort == other.httpPort && this.isLocal == other.isLocal
                && this.metricsPort == other.metricsPort && Objects.equals(this.properties, other.properties);
    }

}
