/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.misc.cloudcat;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CloudCatOptions {

    private static final String RELAY_ENABLE = "relay.enable";
    private static final String FIRST_CLOUD_SERVICE_PID = "first.cloud.service.pid";
    private static final String SECOND_CLOUD_SERVICE_PID = "second.cloud.service.pid";
    private static final String FIRST_CLOUD_CLIENT_APP_ID = "first.cloud.client.app.id";
    private static final String SECOND_CLOUD_CLIENT_APP_ID = "second.cloud.client.app.id";
    private static final String FIRST_CLOUD_CLIENT_CONTROL_SUBSCRPTIONS = "first.cloud.client.control.subscriptions";
    private static final String FIRST_CLOUD_CLIENT_DATA_SUBSCRPTIONS = "first.cloud.client.data.subscriptions";
    private static final String SECOND_CLOUD_CLIENT_CONTROL_SUBSCRPTIONS = "second.cloud.client.control.subscriptions";
    private static final String SECOND_CLOUD_CLIENT_DATA_SUBSCRPTIONS = "second.cloud.client.data.subscriptions";

    private Map<String, Object> effectiveProperties;

    private boolean relayEnabled;
    private String firstCloudServicePid;
    private String secondCloudServicePid;
    private String firstCloudClientAppId;
    private String secondCloudClientAppId;
    private List<CloudCatSubscription> firstCloudClientControlSubscriptions;
    private List<CloudCatSubscription> firstCloudClientDataSubscriptions;
    private List<CloudCatSubscription> secondCloudClientControlSubscriptions;
    private List<CloudCatSubscription> secondCloudClientDataSubscriptions;

    private static CloudCatOptions defaultOptions;
    private static Map<String, Object> defaultProperties;

    public boolean isRelayEnabled() {
        return this.relayEnabled;
    }

    public String getFirstCloudServicePid() {
        return this.firstCloudServicePid;
    }

    public String getSecondCloudServicePid() {
        return this.secondCloudServicePid;
    }

    public String getFirstCloudClientAppId() {
        return this.firstCloudClientAppId;
    }

    public String getSecondCloudClientAppId() {
        return this.secondCloudClientAppId;
    }

    public List<CloudCatSubscription> getFirstCloudClientControlSubscriptions() {
        return this.firstCloudClientControlSubscriptions;
    }

    public List<CloudCatSubscription> getFirstCloudClientDataSubscriptions() {
        return this.firstCloudClientDataSubscriptions;
    }

    public List<CloudCatSubscription> getSecondCloudClientControlSubscriptions() {
        return this.secondCloudClientControlSubscriptions;
    }

    public List<CloudCatSubscription> getSecondCloudClientDataSubscriptions() {
        return this.secondCloudClientDataSubscriptions;
    }

    public static synchronized CloudCatOptions getDefaultOptions() {
        if (defaultOptions == null) {
            defaultOptions = parseOptions(getDefaultProperties());
        }
        return defaultOptions;
    }

    public static CloudCatOptions parseOptions(Map<String, Object> properties) {
        requireNonNull(properties);

        CloudCatOptions options = new CloudCatOptions();
        options.effectiveProperties = new HashMap<>(getDefaultProperties());

        for (Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = properties.get(key);
            if (value != null) {
                options.effectiveProperties.put(key, value);
            }
        }

        options.relayEnabled = (Boolean) options.effectiveProperties.get(RELAY_ENABLE);
        options.firstCloudServicePid = (String) options.effectiveProperties.get(FIRST_CLOUD_SERVICE_PID);
        options.secondCloudServicePid = (String) options.effectiveProperties.get(SECOND_CLOUD_SERVICE_PID);
        options.firstCloudClientAppId = (String) options.effectiveProperties.get(FIRST_CLOUD_CLIENT_APP_ID);
        options.secondCloudClientAppId = (String) options.effectiveProperties.get(SECOND_CLOUD_CLIENT_APP_ID);

        options.firstCloudClientControlSubscriptions = parseSubscriptions(
                (String) options.effectiveProperties.get(FIRST_CLOUD_CLIENT_CONTROL_SUBSCRPTIONS));
        options.firstCloudClientDataSubscriptions = parseSubscriptions(
                (String) options.effectiveProperties.get(FIRST_CLOUD_CLIENT_DATA_SUBSCRPTIONS));
        options.secondCloudClientControlSubscriptions = parseSubscriptions(
                (String) options.effectiveProperties.get(SECOND_CLOUD_CLIENT_CONTROL_SUBSCRPTIONS));
        options.secondCloudClientDataSubscriptions = parseSubscriptions(
                (String) options.effectiveProperties.get(SECOND_CLOUD_CLIENT_DATA_SUBSCRPTIONS));

        return options;
    }

    private static List<CloudCatSubscription> parseSubscriptions(String subscriptions) {
        List<CloudCatSubscription> result = new ArrayList<>();
        String[] parts = subscriptions.split(",");

        if (parts != null) {
            for (String part : parts) {
                if (!part.isEmpty()) {
                    CloudCatSubscription subscription = CloudCatSubscription.parseSubscription(part);
                    result.add(subscription);
                }
            }
        }

        return result;
    }

    private static synchronized Map<String, Object> getDefaultProperties() {
        if (defaultProperties == null) {
            Map<String, Object> properties = new HashMap<>();
            properties.put(RELAY_ENABLE, false);
            properties.put(FIRST_CLOUD_SERVICE_PID, "org.eclipse.kura.cloud.CloudService");
            properties.put(SECOND_CLOUD_SERVICE_PID, "org.eclipse.kura.cloud.CloudService-2");
            properties.put(FIRST_CLOUD_CLIENT_APP_ID, "CLOUDCAT1");
            properties.put(SECOND_CLOUD_CLIENT_APP_ID, "CLOUDCAT2");
            properties.put(FIRST_CLOUD_CLIENT_CONTROL_SUBSCRPTIONS, "");
            properties.put(FIRST_CLOUD_CLIENT_DATA_SUBSCRPTIONS, "");
            properties.put(SECOND_CLOUD_CLIENT_CONTROL_SUBSCRPTIONS, "");
            properties.put(SECOND_CLOUD_CLIENT_DATA_SUBSCRPTIONS, "#;0");

            defaultProperties = properties;
        }
        return defaultProperties;
    }
}
