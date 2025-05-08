/*******************************************************************************
 * Copyright (c) 2019, 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.web.server.util.GwtServerUtil;

public class ConsoleOptions {

    private final SelfConfiguringComponentProperty<String> appRoot = new SelfConfiguringComponentProperty<>(
            new AdBuilder("app.root", "Web Server Entry Point", Tscalar.STRING) //
                    .setDefault("/admin/console") //
                    .setDescription(
                            "This parameter allows to configure the relative path that the user will be redirected to when accessing http(s)://gateway-ip/. Note: this parameter does not change the ESF Web UI relative path, that is always /admin/console.") //
                    .build(), //
            String.class);

    private final SelfConfiguringComponentProperty<Integer> sessionMaxInactivityInterval = new SelfConfiguringComponentProperty<>(
            new AdBuilder("session.max.inactivity.interval", "Session max inactivity interval", Tscalar.INTEGER) //
                    .setDefault("15") //
                    .setMin("1") //
                    .setDescription(
                            "The session max inactivity interval in minutes. If no interaction with the Web UI is performed for the value of this parameter in minutes, a new login will be requested.") //
                    .build(), //
            Integer.class);

    private final SelfConfiguringComponentProperty<Integer[]> allowedPorts = new SelfConfiguringComponentProperty<>(
            new AdBuilder("allowed.ports", "Allowed ports", Tscalar.INTEGER) //
                    .setRequired(false) //
                    .setCardinality(3) //
                    .setDefault("443,4443") //
                    .setDescription(
                            "If set to a non empty list, Web Console access will be allowed only on the specified ports. If set to an empty list, access will be allowed on all ports. Please make sure that the allowed ports are open in HttpService and Firewall configuration.") //
                    .build(), //
            Integer[].class);

    private final SelfConfiguringComponentProperty<String> sslManagerServiceTarget = new SelfConfiguringComponentProperty<>(
            new AdBuilder("SslManagerService.target", "SslManagerService Target Filter", Tscalar.STRING) //
                    .setRequired(true) //
                    .setDefault("(kura.service.pid=org.eclipse.kura.ssl.SslManagerService)") //
                    .setDescription(
                            "Specifies, as an OSGi target filter, the pid of the SslManagerService used to create HTTPS connections. This is needed for example for fetching package descriptions from Eclipse Marketplace.") //
                    .build(),
            String.class);

    private final List<SelfConfiguringComponentProperty<?>> configurationProperties = new ArrayList<>();
    private final Map<String, SelfConfiguringComponentProperty<Boolean>> authenticationMethodProperties = new HashMap<>();
    private final ComponentConfiguration config;

    private ConsoleOptions() {
        initProperties();

        this.config = toComponentConfiguration();
    }

    private ConsoleOptions(final Map<String, Object> properties) {
        initProperties();

        for (final SelfConfiguringComponentProperty<?> property : this.configurationProperties) {
            property.update(properties);
        }

        this.config = toComponentConfiguration();
    }

    public static ConsoleOptions defaultConfiguration() {
        return new ConsoleOptions();
    }

    public static ConsoleOptions fromProperties(final Map<String, Object> properties) {
        return new ConsoleOptions(properties);
    }

    public String getAppRoot() {
        return this.appRoot.get();
    }

    public int getSessionMaxInactivityInterval() {
        return this.sessionMaxInactivityInterval.get();
    }

    public Set<String> getEnabledAuthMethods() {
        return this.authenticationMethodProperties.entrySet().stream().filter(e -> e.getValue().get())
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    public Set<Integer> getAllowedPorts() {
        return GwtServerUtil.getArrayProperty(this.allowedPorts.get(), Integer.class);
    }

    public String getSslManagerServiceTarget() {
        return this.sslManagerServiceTarget.get();
    }

    public boolean isPortAllowed(final int port) {
        final Optional<Integer[]> ports = this.allowedPorts.getOptional();

        if (!ports.isPresent() || ports.get().length == 0) {
            return true;
        }

        for (final Integer allowed : ports.get()) {
            if (allowed != null && allowed == port) {
                return true;
            }
        }

        return false;
    }

    public ComponentConfiguration getConfiguration() {
        return this.config;
    }

    public boolean isAuthenticationMethodEnabled(final String name) {
        final SelfConfiguringComponentProperty<Boolean> property = this.authenticationMethodProperties.get(name);

        if (property == null) {
            return false;
        }

        return property.get();
    }

    private void initProperties() {
        this.configurationProperties.add(this.appRoot);
        this.configurationProperties.add(this.sessionMaxInactivityInterval);
        this.configurationProperties.add(this.allowedPorts);
        this.configurationProperties.add(this.sslManagerServiceTarget);

        addAuthenticationMethodProperties();
    }

    private ComponentConfiguration toComponentConfiguration() {
        final Tocd definition = new Tocd();

        definition.setId("org.eclipse.kura.web.Console");
        definition.setName("WebConsole");
        definition.setDescription(
                "Web Console configuration. A change to this view will case the reload of the web server! Spurious error messages can be displayed during the restart.");

        final Map<String, Object> properties = new HashMap<>();

        for (final SelfConfiguringComponentProperty<?> property : this.configurationProperties) {
            definition.addAD(property.getAd());
            property.fillValue(properties);
        }

        return new ComponentConfigurationImpl("org.eclipse.kura.web.Console", definition, properties);
    }

    private void addAuthenticationMethodProperty(final String name, final boolean enabledByDefault) {
        final SelfConfiguringComponentProperty<Boolean> result = new SelfConfiguringComponentProperty<>(
                new AdBuilder(getAuthenticationMethodPropertyId(name), "Authentication Method \"" + name + "\" Enabled",
                        Tscalar.BOOLEAN) //
                                .setDefault(Boolean.toString(enabledByDefault)) //
                                .setDescription(
                                        "Defines whether the \"" + name + "\" authentication method is enabled or not") //
                                .build(), //
                Boolean.class);

        this.authenticationMethodProperties.put(name, result);
        this.configurationProperties.add(result);

    }

    private static String getAuthenticationMethodPropertyId(final String name) {
        return "auth.method" + name.replace(" ", ".");
    }

    private void addAuthenticationMethodProperties() {
        final Set<String> builtinAuthenticationMethods = Console.instance().getBuiltinAuthenticationMethods();

        for (final String authMethod : Console.instance().getAuthenticationMethods()) {
            addAuthenticationMethodProperty(authMethod, builtinAuthenticationMethods.contains(authMethod));
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.allowedPorts, this.appRoot, this.authenticationMethodProperties,
                this.sessionMaxInactivityInterval, this.sslManagerServiceTarget);
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ConsoleOptions other = (ConsoleOptions) obj;

        return Objects.equals(this.appRoot.get(), other.appRoot.get())
                && Objects.equals(this.sessionMaxInactivityInterval.get(), other.sessionMaxInactivityInterval.get())
                && Arrays.equals(
                        this.allowedPorts.getOptional().isPresent() ? this.allowedPorts.get() : new Integer[] {},
                        other.allowedPorts.getOptional().isPresent() ? other.allowedPorts.get() : new Integer[] {})
                && Objects.equals(this.sslManagerServiceTarget.get(), other.sslManagerServiceTarget.get())
                && Objects.equals(getEnabledAuthMethods(), other.getEnabledAuthMethods());

    }

}
