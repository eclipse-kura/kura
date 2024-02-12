/*******************************************************************************
 * Copyright (c) 2022, 2024 Eurotech and/or its affiliates and others
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

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;

public class TritonServerServiceOptions {

    private static final String PROPERTY_CONTAINER_IMAGE = "container.image";
    private static final String PROPERTY_CONTAINER_TAG = "container.image.tag";
    private static final String CONTAINER_MEMORY = "container.memory";
    private static final String CONTAINER_CPUS = "container.cpus";
    private static final String CONTAINER_GPUS = "container.gpus";
    private static final String CONTAINER_RUNTIME = "container.runtime";
    private static final String PROPERTY_ADDRESS = "server.address";
    private static final String PROPERTY_PORTS = "server.ports";
    private static final String PROPERTY_LOCAL_MODEL_REPOSITORY_PATH = "local.model.repository.path";
    private static final String PROPERTY_LOCAL_MODEL_REPOSITORY_PASSWORD = "local.model.repository.password";
    private static final String PROPERTY_LOCAL_BACKENDS_PATH = "local.backends.path";
    private static final String PROPERTY_LOCAL_BACKENDS_CONFIG = "local.backends.config";
    private static final String PROPERTY_MODELS = "models";
    private static final String PROPERTY_LOCAL = "enable.local";
    private static final String PROPERTY_TIMEOUT = "timeout";
    private static final String PROPERTY_MAX_GRPC_MESSAGE_SIZE = "grpc.max.size";
    private static final String PROPERTY_DEVICES = "devices";
    private final Map<String, Object> properties;

    private static final int RETRY_INTERVAL = 500; // ms
    private static final int DEFAULT_MAX_GRPC_MESSAGE_SIZE = 4194304; // bytes

    private final int httpPort;
    private final int grpcPort;
    private final int metricsPort;
    private final boolean isLocal;
    private final int timeout;
    private final int nRetries;
    private final int grpcMaxMessageSize;
    private final Optional<Long> containerMemory;
    private final Optional<Float> containerCpus;
    private final Optional<String> containerGpus;
    private final Optional<String> containerRuntime;
    private final List<String> devices;

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

        final Object propertyTimeout = this.properties.get(PROPERTY_TIMEOUT);
        if (propertyTimeout instanceof Integer) {
            this.timeout = (Integer) propertyTimeout;
        } else {
            this.timeout = 3;
        }
        this.nRetries = (this.timeout * 1000) / RETRY_INTERVAL;

        final Object propertyGrpcMaxMessageSize = properties.get(PROPERTY_MAX_GRPC_MESSAGE_SIZE);
        if (propertyGrpcMaxMessageSize instanceof Integer) {
            this.grpcMaxMessageSize = (int) propertyGrpcMaxMessageSize;
        } else {
            this.grpcMaxMessageSize = DEFAULT_MAX_GRPC_MESSAGE_SIZE;
        }

        final Object propertyContainerMemory = properties.get(CONTAINER_MEMORY);
        if (propertyContainerMemory instanceof String) {
            this.containerMemory = parseMemoryString(Optional.of((String) propertyContainerMemory));
        } else {
            this.containerMemory = Optional.empty();
        }

        final Object propertyContainerCpus = properties.get(CONTAINER_CPUS);
        if (propertyContainerCpus instanceof Float) {
            this.containerCpus = Optional.of((Float) propertyContainerCpus);
        } else {
            this.containerCpus = Optional.empty();
        }

        final Object propertyContainerGpus = properties.get(CONTAINER_GPUS);
        if (propertyContainerGpus instanceof String) {
            this.containerGpus = parseOptionalString(Optional.of((String) propertyContainerGpus));
        } else {
            this.containerGpus = Optional.empty();
        }

        final Object propertyContainerRuntime = properties.get(CONTAINER_RUNTIME);
        if (propertyContainerRuntime instanceof String) {
            this.containerRuntime = parseOptionalString(Optional.of((String) propertyContainerRuntime));
        } else {
            this.containerRuntime = Optional.empty();
        }

        final Object propertyDevices = properties.get(PROPERTY_DEVICES);
        if (propertyDevices instanceof String) {
            this.devices = parseDevices((String) propertyDevices);
        } else {
            this.devices = new ArrayList<>();
        }

    }

    public String getAddress() {
        String address = "";
        final Object propertyAddress = this.properties.get(PROPERTY_ADDRESS);
        if (propertyAddress instanceof String) {
            address = ((String) propertyAddress).trim();
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

    public String getContainerImage() {
        return getStringProperty(PROPERTY_CONTAINER_IMAGE);
    }

    public String getContainerImageTag() {
        return getStringProperty(PROPERTY_CONTAINER_TAG);
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

    public boolean isModelEncryptionPasswordSet() {
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

    public int getNRetries() {
        return this.nRetries;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public int getRetryInterval() {
        return RETRY_INTERVAL;
    }

    public int getGrpcMaxMessageSize() {
        return this.grpcMaxMessageSize;
    }

    public Optional<Long> getContainerMemory() {
        return this.containerMemory;
    }

    public Optional<Float> getContainerCpus() {
        return this.containerCpus;
    }

    public Optional<String> getContainerGpus() {
        return this.containerGpus;
    }

    public Optional<String> getContainerRuntime() {
        return this.containerRuntime;
    }

    public List<String> getDevices() {
        return this.devices;
    }

    private String getStringProperty(String propertyName) {
        String stringProperty = "";
        final Object stringPropertyObj = this.properties.get(propertyName);
        if (stringPropertyObj instanceof String) {
            stringProperty = (String) stringPropertyObj;
        }
        return stringProperty;
    }

    private Optional<Long> parseMemoryString(Optional<String> value) {
        if (value.isPresent() && !value.get().trim().isEmpty()) {
            String stringValue = value.get().trim();
            long result = 0;
            switch (stringValue.charAt(stringValue.length() - 1)) {
            case 'b':
                result = Long.parseLong(stringValue.substring(0, stringValue.length() - 1));
                break;
            case 'k':
                result = Long.parseLong(stringValue.substring(0, stringValue.length() - 1)) * 1024L;
                break;
            case 'm':
                result = Long.parseLong(stringValue.substring(0, stringValue.length() - 1)) * 1048576L;
                break;
            case 'g':
                result = Long.parseLong(stringValue.substring(0, stringValue.length() - 1)) * 1073741824L;
                break;
            default:
                result = Long.parseLong(stringValue);
            }
            return Optional.of(result);
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> parseOptionalString(Optional<String> optionalString) {
        if (optionalString.isPresent() && optionalString.get().isEmpty()) {
            return Optional.empty();
        } else {
            return optionalString;
        }
    }

    private List<String> parseDevices(String devicesString) {
        if (isNull(devicesString) || devicesString.isEmpty()) {
            return new ArrayList<>();
        } else {
            return Arrays.asList(devicesString.trim().split(","));
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.grpcPort, this.httpPort, this.isLocal, this.metricsPort, this.timeout, this.nRetries,
                this.properties);
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
                && this.metricsPort == other.metricsPort && this.timeout == other.timeout
                && this.containerCpus == other.containerCpus && this.containerGpus == other.containerGpus
                && this.containerMemory == other.containerMemory && this.nRetries == other.nRetries
                && Objects.equals(this.properties, other.properties);
    }

}
