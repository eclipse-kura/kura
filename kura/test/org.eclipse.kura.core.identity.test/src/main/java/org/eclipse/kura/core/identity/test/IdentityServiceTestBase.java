/*******************************************************************************
 * Copyright (c) 2024, 2025 Eurotech and/or its affiliates and others
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.testutil.service.ServiceUtil;

public abstract class IdentityServiceTestBase {

    private static final String PASSWORD_STRENGTH_VERIFICATION_SERVICE_PID = "org.eclipse.kura.identity.PasswordStrengthVerificationService";

    private final ConfigurationService configurationService;

    protected void givenPasswordStrenghtVerificationOptions(final Object... values) {

        final Iterator<Object> iter = Arrays.asList(values).iterator();

        final Map<String, Object> properties = new HashMap<>();

        while (iter.hasNext()) {
            properties.put((String) iter.next(), iter.next());
        }

        try {
            ServiceUtil.updateComponentConfiguration(configurationService, PASSWORD_STRENGTH_VERIFICATION_SERVICE_PID,
                    properties).get(30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            fail("failed to update authentication settings");
            throw new IllegalStateException("unreachable");
        }
    }

    public IdentityServiceTestBase() {
        try {
            this.configurationService = ServiceUtil.trackService(ConfigurationService.class, Optional.empty()).get(30,
                    TimeUnit.SECONDS);

            ServiceUtil
                    .trackService(ConfigurableComponent.class,
                            Optional.of("(kura.service.pid=" + PASSWORD_STRENGTH_VERIFICATION_SERVICE_PID + ")"))
                    .get(30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            fail("failed to track ConfigurationService");
            throw new IllegalStateException("unreachable");
        }
    }

}
