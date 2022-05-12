/*******************************************************************************
 * Copyright (c) 2017, 2022 Eurotech and/or its affiliates and others
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
    private static final Property<Boolean> CERTIFICATE_ENABLED = new Property<>("auth.certificate.enabled", true);

    private final Set<Integer> allowedPorts;
    private final boolean passwordAuthEnabled;
    private final boolean certificateAuthEnabled;

    public RestServiceOptions(final Map<String, Object> properties) {
        this.allowedPorts = loadIntArrayProperty(ALLOWED_PORTS.get(properties));
        this.passwordAuthEnabled = PASSWORD_AUTH_ENABLED.get(properties);
        this.certificateAuthEnabled = CERTIFICATE_ENABLED.get(properties);
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
        return Objects.hash(allowedPorts, certificateAuthEnabled, passwordAuthEnabled);
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
        return Objects.equals(allowedPorts, other.allowedPorts)
                && certificateAuthEnabled == other.certificateAuthEnabled
                && passwordAuthEnabled == other.passwordAuthEnabled;
    }

}