/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.example.identity.configuration.extension.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.configuration.metatype.Scalar;
import org.eclipse.kura.identity.configuration.extension.IdentityConfigurationExtension;
import org.eclipse.kura.util.wire.test.WireTestUtil;
import org.junit.After;
import org.junit.Test;

public class ExampleIdentityConfigurationExtensionTest {

    private static final String FACTORY_PID = "org.eclipse.kura.example.identity.configuration.extension.ExampleIdentityConfigurationExtension";
    private static final String TEST_EXTENSION_PID = "testExtension";

    private final ConfigurationService configurationService;
    private IdentityConfigurationExtension extension;

    private Optional<ComponentConfiguration> receivedConfiguration = Optional.empty();
    private Optional<ComponentConfiguration> configurationToUpdate = Optional.empty();
    private Optional<Exception> exception = Optional.empty();

    @Test
    public void shouldReturnDefaultIdentityConfigurationProperties() {
        givenExampleIdentityConfigurationExtension();

        whenConfigurationForIdentityIsRetrieved("foo");

        thenNoExceptionIsThrown();
        thenReceivedConfigurationPropertyIs("test.string", "foobar");
        thenReceivedConfigurationPropertyIs("test.integer", 0);
    }

    @Test
    public void shouldReturnOCD() {
        givenExampleIdentityConfigurationExtension();

        whenConfigurationForIdentityIsRetrieved("foo");

        thenNoExceptionIsThrown();

        thenAttributeDefinitionComponentIs("test.string", AD::getName, "Test String Property");
        thenAttributeDefinitionComponentIs("test.string", AD::getDescription, "A test string property");
        thenAttributeDefinitionComponentIs("test.string", AD::getDefault, "foobar");
        thenAttributeDefinitionComponentIs("test.string", AD::isRequired, true);
        thenAttributeDefinitionComponentIs("test.string", AD::getType, Scalar.STRING);

        thenAttributeDefinitionComponentIs("test.integer", AD::getName, "Test Integer Property");
        thenAttributeDefinitionComponentIs("test.integer", AD::getDescription, "A test integer property");
        thenAttributeDefinitionComponentIs("test.integer", AD::getDefault, "0");
        thenAttributeDefinitionComponentIs("test.integer", AD::isRequired, true);
        thenAttributeDefinitionComponentIs("test.integer", AD::getType, Scalar.INTEGER);

    }

    @Test
    public void shouldRememberUpdatedValues() {
        givenExampleIdentityConfigurationExtension();
        givenUpdateProperty("foo", "test.string", "baz");
        givenUpdateProperty("foo", "test.integer", 5);
        givenSuccessfulConfigurationUpdate("foo");

        whenConfigurationForIdentityIsRetrieved("foo");

        thenNoExceptionIsThrown();
        thenReceivedConfigurationPropertyIs("test.string", "baz");
        thenReceivedConfigurationPropertyIs("test.integer", 5);

    }

    public ExampleIdentityConfigurationExtensionTest() {
        try {
            this.configurationService = WireTestUtil.trackService(ConfigurationService.class, Optional.empty()).get(30,
                    TimeUnit.SECONDS);
        } catch (final Exception e) {
            fail("failed to track ConfigurationService");
            throw new IllegalStateException("unreachable");
        }
    }

    private void givenUpdateProperty(final String identityName, final String key, final Object value) {
        final ComponentConfiguration config;

        final Optional<ComponentConfiguration> currentConfigToUpdate = this.configurationToUpdate;

        if (currentConfigToUpdate.isPresent()) {
            config = currentConfigToUpdate.get();
        } else {
            final Map<String, Object> properties = new HashMap<>();
            config = new ComponentConfiguration() {

                @Override
                public String getPid() {
                    return TEST_EXTENSION_PID;
                }

                @Override
                public OCD getDefinition() {
                    return null;
                }

                @Override
                public Map<String, Object> getConfigurationProperties() {

                    return properties;
                }
            };

            this.configurationToUpdate = Optional.of(config);
        }

        config.getConfigurationProperties().put(key, value);
    }

    private void givenExampleIdentityConfigurationExtension() {
        try {
            this.extension = WireTestUtil.createFactoryConfiguration(this.configurationService,
                    IdentityConfigurationExtension.class, TEST_EXTENSION_PID, FACTORY_PID, Collections.emptyMap())
                    .get(30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            fail("failed to create example identiy configuration extension");
        }
    }

    private void givenSuccessfulConfigurationUpdate(final String name) {
        whenConfigurationIsUpdated(name);
        thenNoExceptionIsThrown();
    }

    private void whenConfigurationIsUpdated(final String name) {
        try {
            this.extension.updateConfiguration(name, this.configurationToUpdate
                    .orElseThrow(() -> new IllegalStateException("configuration to update is not initialized")));
            this.exception = Optional.empty();
        } catch (Exception e) {
            this.exception = Optional.of(e);
        }
    }

    private void whenConfigurationForIdentityIsRetrieved(final String name) {
        try {
            this.receivedConfiguration = this.extension.getConfiguration(name);
            this.exception = Optional.empty();
        } catch (Exception e) {
            this.exception = Optional.of(e);
        }
    }

    private void thenNoExceptionIsThrown() {
        assertEquals(Optional.empty(), this.exception);
    }

    private void thenReceivedConfigurationPropertyIs(final String key, final Object value) {
        assertEquals(Optional.of(value), this.receivedConfiguration.map(c -> c.getConfigurationProperties().get(key)));
    }

    private <T> void thenAttributeDefinitionComponentIs(final String adId, final Function<AD, T> component,
            final T value) {
        assertEquals(Optional.of(value), this.receivedConfiguration.flatMap(c -> c.getDefinition().getAD().stream()
                .filter(a -> Objects.equals(adId, a.getId())).map(component).findAny()));
    }

    @After
    public void cleanUp() {
        try {
            if (this.extension != null) {
                WireTestUtil.deleteFactoryConfiguration(this.configurationService, TEST_EXTENSION_PID).get(30,
                        TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            fail("failed to delete configuration");
        }

    }
}
