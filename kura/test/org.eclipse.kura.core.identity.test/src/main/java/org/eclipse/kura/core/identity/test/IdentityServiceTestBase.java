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
 *******************************************************************************/
package org.eclipse.kura.core.identity.test;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.testutil.service.ServiceUtil;
import org.junit.After;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

public abstract class IdentityServiceTestBase {

    protected void givenConsoleOptions(final Object... values) {

        final Iterator<Object> iter = Arrays.asList(values).iterator();

        final Map<String, Object> properties = new HashMap<>();

        while (iter.hasNext()) {
            properties.put((String) iter.next(), iter.next());
        }

        this.console.setProperties(properties);
    }

    private static final String CONSOLE_PID = "org.eclipse.kura.web.Console";

    private final MockConsole console = new MockConsole();

    private final ServiceRegistration<SelfConfiguringComponent> reg;

    public IdentityServiceTestBase() {
        try {
            this.reg = console.register();
            ServiceUtil
                    .trackService(SelfConfiguringComponent.class, Optional.of("(kura.service.pid=" + CONSOLE_PID + ")"))
                    .get(30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            fail("failed to track ConfigurationService");
            throw new IllegalStateException("unreachable");
        }
    }

    @After
    public void unregisterConsoleComponent() {
        this.reg.unregister();
    }

    private static class MockConsole implements SelfConfiguringComponent {

        private Map<String, Object> properties = new HashMap<>();

        public MockConsole() {
        }

        @Override
        public ComponentConfiguration getConfiguration() throws KuraException {

            return new ComponentConfiguration() {

                @Override
                public String getPid() {
                    return CONSOLE_PID;
                }

                @Override
                public OCD getDefinition() {
                    return new Tocd();
                }

                @Override
                public Map<String, Object> getConfigurationProperties() {
                    properties.put(ConfigurationService.KURA_SERVICE_PID, CONSOLE_PID);
                    properties.put("service.pid", CONSOLE_PID);

                    return properties;
                }
            };
        }

        void setProperties(final Map<String, Object> properties) {
            this.properties = properties;
        }

        ServiceRegistration<SelfConfiguringComponent> register() {
            final Dictionary<String, Object> serviceProperties = new Hashtable<>();
            serviceProperties.put(ConfigurationService.KURA_SERVICE_PID, CONSOLE_PID);
            serviceProperties.put("service.pid", CONSOLE_PID);

            return FrameworkUtil.getBundle(PasswordStrengthVerificationServiceImplTest.class).getBundleContext()
                    .registerService(SelfConfiguringComponent.class, this, serviceProperties);
        }
    }
}
