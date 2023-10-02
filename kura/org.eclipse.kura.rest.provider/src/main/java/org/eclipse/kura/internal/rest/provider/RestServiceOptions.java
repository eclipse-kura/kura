/*******************************************************************************
 * Copyright (c) 2017, 2023 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.internal.rest.provider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.kura.util.configuration.Property;

public class RestServiceOptions {

    private static final Property<Integer[]> ALLOWED_PORTS = new Property<>("allowed.ports", new Integer[] {});
    private static final Property<Boolean> PASSWORD_AUTH_ENABLED = new Property<>("auth.password.enabled", true);
    private static final Property<Boolean> CERTIFICATE_AUTH_ENABLED = new Property<>("auth.certificate.enabled", true);
    private static final Property<Boolean> SESSION_MANAGEMENT_ENABLED = new Property<>("session.management.enabled",
            true);
    private static final Property<Integer> SESSION_INACTIVITY_INTERVAL = new Property<>("session.inactivity.interval",
            900);
    private static final Property<Boolean> BASIC_AUTHENTICATION_ENABLED = new Property<>("auth.basic.enabled", true);
    private static final Property<Boolean> STATELESS_CERTIFICATE_AUTHENTICATION_ENABLED = new Property<>(
            "auth.certificate.stateless.enabled",
            true);

    private final Set<Integer> allowedPorts;
    private final boolean passwordAuthEnabled;
    private final boolean certificateAuthEnabled;
    private final boolean sessionManagementEnabled;
    private final int sessionInactivityInterval;
    private final boolean basicAuthEnabled;
    private final boolean statelessCertificateAuthEnabled;

    public RestServiceOptions(final Map<String, Object> properties) {
        this.allowedPorts = loadIntArrayProperty(ALLOWED_PORTS.get(properties));
        this.passwordAuthEnabled = PASSWORD_AUTH_ENABLED.get(properties);
        this.certificateAuthEnabled = CERTIFICATE_AUTH_ENABLED.get(properties);
        this.sessionManagementEnabled = SESSION_MANAGEMENT_ENABLED.get(properties);
        this.sessionInactivityInterval = SESSION_INACTIVITY_INTERVAL.get(properties);
        this.basicAuthEnabled = BASIC_AUTHENTICATION_ENABLED.get(properties);
        this.statelessCertificateAuthEnabled = STATELESS_CERTIFICATE_AUTHENTICATION_ENABLED.get(properties);
    }

    public Set<Integer> getAllowedPorts() {
        return allowedPorts;
    }

    public boolean isPasswordAuthEnabled() {
        return passwordAuthEnabled;
    }

    public boolean isCertificateAuthEnabled() {
        return certificateAuthEnabled;
    }

    public boolean isSessionManagementEnabled() {
        return sessionManagementEnabled;
    }

    public int getSessionInactivityInterval() {
        return sessionInactivityInterval;
    }

    public boolean isBasicAuthEnabled() {
        return basicAuthEnabled;
    }

    public boolean isStatelessCertificateAuthEnabled() {
        return statelessCertificateAuthEnabled;
    }

    private static Set<Integer> loadIntArrayProperty(final Integer[] list) {
        if (list == null) {
            return Collections.emptySet();
        }

        final Set<Integer> result = new HashSet<>();

        for (int i = 0; i < list.length; i++) {
            final Integer value = list[i];

            if (value != null) {
                result.add(value);
            }
        }

        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowedPorts, basicAuthEnabled, certificateAuthEnabled, passwordAuthEnabled,
                sessionInactivityInterval, sessionManagementEnabled, statelessCertificateAuthEnabled);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RestServiceOptions)) {
            return false;
        }
        RestServiceOptions other = (RestServiceOptions) obj;
        return Objects.equals(allowedPorts, other.allowedPorts) && basicAuthEnabled == other.basicAuthEnabled
                && certificateAuthEnabled == other.certificateAuthEnabled
                && passwordAuthEnabled == other.passwordAuthEnabled
                && sessionInactivityInterval == other.sessionInactivityInterval
                && sessionManagementEnabled == other.sessionManagementEnabled
                && statelessCertificateAuthEnabled == other.statelessCertificateAuthEnabled;
    }

}
