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
package org.eclipse.kura.example.identity.configuration.extension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.identity.configuration.extension.IdentityConfigurationExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleIdentityConfigurationExtension implements IdentityConfigurationExtension, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(ExampleIdentityConfigurationExtension.class);

    private final Map<String, IdentityConfiguration> configurations = new HashMap<>();

    private String kuraServicePid;

    public void activate(final Map<String, Object> properties) {
        logger.info("activating...");

        updated(properties);

        logger.info("activating...done");
    }

    public void updated(final Map<String, Object> properties) {
        logger.info("updating...");

        try {
            this.kuraServicePid = (String) properties.get(ConfigurationService.KURA_SERVICE_PID);
        } catch (final Exception e) {
            logger.warn("failed to get own " + ConfigurationService.KURA_SERVICE_PID, e);
        }

        logger.info("updating...done");
    }

    @Override
    public synchronized Optional<ComponentConfiguration> getConfiguration(final String identityName)
            throws KuraException {

        final IdentityConfiguration result = Optional.ofNullable(this.configurations.get(identityName))
                .orElseGet(IdentityConfiguration::new);

        return Optional.of(buildComponentConfiguration(result));
    }

    @Override
    public Optional<ComponentConfiguration> getDefaultConfiguration(String identityName) throws KuraException {
        return Optional.of(buildComponentConfiguration(new IdentityConfiguration()));
    }

    private ComponentConfiguration buildComponentConfiguration(final IdentityConfiguration configuration) {
        return new ComponentConfiguration() {

            @Override
            public String getPid() {
                return ExampleIdentityConfigurationExtension.this.kuraServicePid;
            }

            @Override
            public OCD getDefinition() {
                return IdentityConfiguration.buildOCD(ExampleIdentityConfigurationExtension.this.kuraServicePid);
            }

            @Override
            public Map<String, Object> getConfigurationProperties() {
                return configuration.toProperties();
            }
        };
    }

    @Override
    public void validateConfiguration(String identityName, ComponentConfiguration configuration) throws KuraException {
        final IdentityConfiguration config = new IdentityConfiguration(configuration.getConfigurationProperties());

        if (!config.testStringProperty.matches("[a-zA-Z]+")) {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER,
                    "test.string value must contain only uppercase or lowercase letters");
        }
    }

    @Override
    public synchronized void updateConfiguration(String identityName, ComponentConfiguration configuration)
            throws KuraException {

        validateConfiguration(identityName, configuration);

        final IdentityConfiguration config = new IdentityConfiguration(configuration.getConfigurationProperties());

        logger.info("received configuration for identity {}: {}", identityName, config);

        if ("failure".equals(config.testStringProperty)) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "failed to update configuration");
        }

        this.configurations.put(identityName, config);

    }

    private static class IdentityConfiguration {

        private static final String TEST_STRING_PROPERTY_KEY = "test.string";
        private static final String TEST_INTEGER_PROPERTY_KEY = "test.integer";

        private static final String TEST_STRING_PROPERTY_DEFAULT = "foobar";
        private static final int TEST_INTEGER_PROPERTY_DEFAULT = 0;

        private final String testStringProperty;
        private final int testIntProperty;

        IdentityConfiguration() {
            this.testStringProperty = TEST_STRING_PROPERTY_DEFAULT;
            this.testIntProperty = TEST_INTEGER_PROPERTY_DEFAULT;
        }

        IdentityConfiguration(final Map<String, Object> properties) {
            this.testStringProperty = Optional.ofNullable(properties.get(TEST_STRING_PROPERTY_KEY))
                    .filter(String.class::isInstance).map(String.class::cast).orElse(TEST_STRING_PROPERTY_DEFAULT);
            this.testIntProperty = Optional.ofNullable(properties.get(TEST_INTEGER_PROPERTY_KEY))
                    .filter(Integer.class::isInstance).map(Integer.class::cast).orElse(TEST_INTEGER_PROPERTY_DEFAULT);
        }

        Map<String, Object> toProperties() {
            final Map<String, Object> result = new HashMap<>();

            result.put(TEST_STRING_PROPERTY_KEY, this.testStringProperty);
            result.put(TEST_INTEGER_PROPERTY_KEY, this.testIntProperty);

            return result;
        }

        @Override
        public String toString() {
            return "IdentityConfiguration [testStringProperty=" + this.testStringProperty + ", testIntProperty="
                    + this.testIntProperty + "]";
        }

        static OCD buildOCD(final String kuraServicePid) {
            final Tocd ocd = new Tocd();

            ocd.setId(kuraServicePid);
            ocd.setName("Example Configuration Extension " + kuraServicePid);
            ocd.setDescription("Configuration provided by example configuration extension " + kuraServicePid);

            final Tad testStringProp = new Tad();
            testStringProp.setId(TEST_STRING_PROPERTY_KEY);
            testStringProp.setName("Test String Property");
            testStringProp.setDescription("A test string property");
            testStringProp.setDefault(TEST_STRING_PROPERTY_DEFAULT);
            testStringProp.setRequired(true);
            testStringProp.setType(Tscalar.STRING);
            ocd.addAD(testStringProp);

            final Tad testIntProp = new Tad();
            testIntProp.setId(TEST_INTEGER_PROPERTY_KEY);
            testIntProp.setName("Test Integer Property");
            testIntProp.setDescription("A test integer property");
            testIntProp.setDefault(Integer.toString(TEST_INTEGER_PROPERTY_DEFAULT));
            testIntProp.setRequired(true);
            testIntProp.setType(Tscalar.INTEGER);
            ocd.addAD(testIntProp);

            return ocd;
        }

    }
}
