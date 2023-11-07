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
package org.eclipse.kura.internal.rest.auth;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationAdminHelper {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationAdminHelper.class);

    private ConfigurationAdminHelper() {
    }

    public static Map<String, Object> loadConfigurationProperties(final ConfigurationAdmin configurationAdmin,
            final String pid) {

        return getConfiguration(configurationAdmin, pid)
                .map(Configuration::getProperties).map(ConfigurationAdminHelper::dictionaryToMap)
                .orElseGet(() -> new HashMap<>());
    }

    public static Map<String, Object> loadConsoleConfigurationProperties(final ConfigurationAdmin configurationAdmin) {

        return loadConfigurationProperties(configurationAdmin, "org.eclipse.kura.web.Console");
    }

    public static Map<String, Object> loadHttpServiceConfigurationProperties(
            final ConfigurationAdmin configurationAdmin) {

        return loadConfigurationProperties(configurationAdmin, "org.eclipse.kura.http.server.manager.HttpService");
    }

    public static Optional<String> getLoginMessage(final Map<String, Object> properties) {
        final Object messageEnabled = properties.get("access.banner.enabled");
        final Object message = properties.get("access.banner.content");

        if (!Boolean.TRUE.equals(messageEnabled)) {
            return Optional.empty();
        }

        if (!(message instanceof String)) {
            return Optional.empty();
        }

        return Optional.of((String) message);
    }

    public static Set<Integer> getHttpsMutualAuthPorts(final Map<String, Object> properties) {
        final Object rawPortList = properties.get("https.client.auth.ports");

        if (!(rawPortList instanceof Integer[])) {
            return Collections.emptySet();
        }

        final Integer[] portList = (Integer[]) rawPortList;

        return Arrays.stream(portList).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static Optional<Configuration> getConfiguration(final ConfigurationAdmin configurationAdmin,
            final String pid) {
        try {
            return Optional
                    .ofNullable(configurationAdmin.getConfiguration(pid, "?"));
        } catch (final IOException e) {
            logger.warn("Failed to retrieve configuration for {}", pid, e);
            return Optional.empty();
        }
    }

    private static final Map<String, Object> dictionaryToMap(final Dictionary<String, Object> dict) {
        final Map<String, Object> result = new HashMap<>(dict.size());

        final Enumeration<String> keys = dict.keys();

        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();

            result.put(key, dict.get(key));
        }

        return result;
    }
}
