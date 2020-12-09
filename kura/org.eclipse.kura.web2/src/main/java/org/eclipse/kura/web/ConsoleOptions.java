/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.web.shared.model.GwtConsoleUserOptions;

public class ConsoleOptions {

    private final SelfConfiguringComponentProperty<String> username = new SelfConfiguringComponentProperty<>(
            new AdBuilder("console.username.value", "Console Username", Tscalar.STRING) //
                    .setDefault("admin") //
                    .setDescription("Device's web username.") //
                    .build(), //
            String.class);

    private final SelfConfiguringComponentProperty<String> userPassword;

    private final SelfConfiguringComponentProperty<String> appRoot = new SelfConfiguringComponentProperty<>(
            new AdBuilder("app.root", "Web Server Entry Point", Tscalar.STRING) //
                    .setDefault("/admin/console") //
                    .setDescription(
                            "Default Web Server Entry Point for Applications running on the framework's Web Server") //
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

    private final SelfConfiguringComponentProperty<Boolean> bannerEnabled = new SelfConfiguringComponentProperty<>(
            new AdBuilder("access.banner.enabled", "Access Banner Enabled", Tscalar.BOOLEAN) //
                    .setDefault("false") //
                    .setDescription("Enables or disables the displaying of a customizable banner at user login.") //
                    .build(), //
            Boolean.class);

    private final SelfConfiguringComponentProperty<String> bannerContent = new SelfConfiguringComponentProperty<>(
            new AdBuilder("access.banner.content", "Access Banner Content", Tscalar.STRING) //
                    .setDefault("Sample Banner Content") //
                    .setDescription(
                            "Access banner content. To be displayed at every user access, if the feature is enabled.|TextArea") //
                    .build(), //
            String.class);

    private final SelfConfiguringComponentProperty<Integer> passwordMinLength = new SelfConfiguringComponentProperty<>(
            new AdBuilder("new.password.min.length", "Minimum password length", Tscalar.INTEGER) //
                    .setDefault("8") //
                    .setMin("0") //
                    .setDescription("The minimum length to be enforced for new passwords. Set to 0 to disable.") //
                    .build(), //
            Integer.class);

    private final SelfConfiguringComponentProperty<Boolean> passwordRequireDigits = new SelfConfiguringComponentProperty<>(
            new AdBuilder("new.password.require.digits", "Require digits in new password", Tscalar.BOOLEAN) //
                    .setDefault("false") //
                    .setDescription(
                            "If set to true, new passwords will be accepted only if containing at least one digit.") //
                    .build(), //
            Boolean.class);

    private final SelfConfiguringComponentProperty<Boolean> passwordRequireSpecialCharacters = new SelfConfiguringComponentProperty<>(
            new AdBuilder("new.password.require.special.characters", "Require special characters in new password",
                    Tscalar.BOOLEAN) //
                            .setDefault("false") //
                            .setDescription(
                                    "If set to true, new passwords will be accepted only if containing at least one non alphanumeric character.") //
                            .build(), //
            Boolean.class);

    private final SelfConfiguringComponentProperty<Boolean> passwordRequireBothCases = new SelfConfiguringComponentProperty<>(
            new AdBuilder("new.password.require.both.cases",
                    "Require uppercase and lowercase characters in new passwords", Tscalar.BOOLEAN) //
                            .setDefault("false") //
                            .setDescription(
                                    "If set to true, new passwords will be accepted only if containing both uppercase and lowercase alphanumeric characters.") //
                            .build(), //
            Boolean.class);

    private final List<SelfConfiguringComponentProperty<?>> configurationProperties = new ArrayList<>();
    private final Map<String, SelfConfiguringComponentProperty<Boolean>> authenticationMethodProperties = new HashMap<>();
    private final ComponentConfiguration config;

    private ConsoleOptions(final CryptoService cryptoService) {
        this.userPassword = initPasswordProperty(cryptoService);

        initProperties();

        this.config = toComponentConfiguration();
    }

    private ConsoleOptions(final Map<String, Object> properties, final CryptoService cryptoService) {
        this.userPassword = initPasswordProperty(cryptoService);

        initProperties();

        for (final SelfConfiguringComponentProperty<?> property : configurationProperties) {
            property.update(properties);
        }

        this.config = toComponentConfiguration();
    }

    public static ConsoleOptions defaultConfiguration(final CryptoService cryptoService) {
        return new ConsoleOptions(cryptoService);
    }

    public static ConsoleOptions fromProperties(final Map<String, Object> properties,
            final CryptoService cryptoService) {
        return new ConsoleOptions(properties, cryptoService);
    }

    public String getUsername() {
        return this.username.get();
    }

    public String getUserPassword() {
        return this.userPassword.get();
    }

    public String getAppRoot() {
        return this.appRoot.get();
    }

    public int getSessionMaxInactivityInterval() {
        return this.sessionMaxInactivityInterval.get();
    }

    public boolean isBannerEnabled() {
        return this.bannerEnabled.get();
    }

    public String getBannerContent() {
        return this.bannerContent.get();
    }

    public Set<String> getEnabledAuthMethods() {
        return authenticationMethodProperties.entrySet().stream().filter(e -> e.getValue().get()).map(e -> e.getKey())
                .collect(Collectors.toSet());
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

    public GwtConsoleUserOptions getUserOptions() {
        final GwtConsoleUserOptions result = new GwtConsoleUserOptions();

        result.setPasswordMinimumLength(passwordMinLength.get());
        result.setPasswordRequireDigits(passwordRequireDigits.get());
        result.setPasswordRequireSpecialChars(passwordRequireSpecialCharacters.get());
        result.setPasswordRequireBothCases(passwordRequireBothCases.get());

        return result;
    }

    private void initProperties() {
        configurationProperties.add(username);
        configurationProperties.add(userPassword);
        configurationProperties.add(appRoot);
        configurationProperties.add(sessionMaxInactivityInterval);
        configurationProperties.add(bannerEnabled);
        configurationProperties.add(bannerContent);
        configurationProperties.add(passwordMinLength);
        configurationProperties.add(passwordRequireDigits);
        configurationProperties.add(passwordRequireSpecialCharacters);
        configurationProperties.add(passwordRequireBothCases);

        addAuthenticationMethodProperties();
    }

    private static SelfConfiguringComponentProperty<String> initPasswordProperty(final CryptoService cryptoService) {

        return new SelfConfiguringComponentProperty<>(
                new AdBuilder("console.password.value", "Console Password", Tscalar.PASSWORD) //
                        .setDefault("admin") //
                        .setDescription("Device's web console password. A non empty value should be set.") //
                        .build(), //
                String.class, cryptoService);
    }

    private ComponentConfiguration toComponentConfiguration() {
        final Tocd definition = new Tocd();

        definition.setId("org.eclipse.kura.web.Console");
        definition.setName("WebConsole");
        definition.setDescription(
                "Web Console configuration. A change to this view will case the reload of the web server! Spurious error messages can be displayed during the restart.");

        final Map<String, Object> properties = new HashMap<>();

        for (final SelfConfiguringComponentProperty<?> property : configurationProperties) {
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

        authenticationMethodProperties.put(name, result);
        configurationProperties.add(result);

    }

    private static String getAuthenticationMethodPropertyId(final String name) {
        return "auth.method" + name.replaceAll(" ", ".");
    }

    private void addAuthenticationMethodProperties() {
        final Set<String> builtinAuthenticationMethods = Console.instance().getBuiltinAuthenticationMethods();

        for (final String authMethod : Console.instance().getAuthenticationMethods()) {
            addAuthenticationMethodProperty(authMethod, builtinAuthenticationMethods.contains(authMethod));
        }
    }

}
