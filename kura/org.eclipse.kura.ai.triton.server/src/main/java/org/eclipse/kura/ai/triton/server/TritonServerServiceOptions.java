package org.eclipse.kura.ai.triton.server;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TritonServerServiceOptions {

    private static final String PROPERTY_ADDRESS = "server.address";
    private static final String PROPERTY_PORTS = "server.ports";
    private static final String PROPERTY_LOCAL_MODEL_REPOSITORY_PATH = "local.model.repository.path";
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
        if (nonNull(propertyPorts) && propertyPorts instanceof Integer[]) {
            Integer[] ports = (Integer[]) propertyPorts;
            this.httpPort = ports[0];
            this.grpcPort = ports[1];
            this.metricsPort = ports[2];
        } else {
            this.httpPort = 5000;
            this.grpcPort = 5001;
            this.metricsPort = 5002;
        }

        final Object propertyLocal = this.properties.get(PROPERTY_LOCAL);
        if (nonNull(propertyLocal) && propertyLocal instanceof Boolean) {
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
            if (nonNull(propertyAddress) && propertyAddress instanceof String) {
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
        return getPath(PROPERTY_LOCAL_MODEL_REPOSITORY_PATH);
    }

    public String getBackendsPath() {
        return getPath(PROPERTY_LOCAL_BACKENDS_PATH);
    }

    public List<String> getBackendsConfigs() {
        List<String> backendsConfigs = new ArrayList<>();
        final Object propertyBackendsConfig = this.properties.get(PROPERTY_LOCAL_BACKENDS_CONFIG);
        if (nonNull(propertyBackendsConfig) && propertyBackendsConfig instanceof String
                && !((String) propertyBackendsConfig).isEmpty()) {
            backendsConfigs = Arrays.asList(((String) propertyBackendsConfig).trim().split(";"));
        }
        return backendsConfigs;
    }

    public List<String> getModels() {
        List<String> models = new ArrayList<>();
        final Object propertyModels = this.properties.get(PROPERTY_MODELS);
        if (nonNull(propertyModels) && propertyModels instanceof String && !((String) propertyModels).isEmpty()) {
            models = Arrays.asList(((String) propertyModels).replace(" ", "").split(","));
        }
        return models;
    }

    private String getPath(String propertyName) {
        String path = "";
        final Object propertyPath = this.properties.get(propertyName);
        if (nonNull(propertyPath) && propertyPath instanceof String) {
            path = (String) propertyPath;
        }
        return path;
    }
}
