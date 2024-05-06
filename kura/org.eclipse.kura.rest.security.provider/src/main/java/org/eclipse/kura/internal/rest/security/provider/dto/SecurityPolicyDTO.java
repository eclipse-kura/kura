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
package org.eclipse.kura.internal.rest.security.provider.dto;

public class SecurityPolicyDTO {

    private final String securityPolicy;

    public SecurityPolicyDTO(String securityPolicy) {
        requireNotEmpty(securityPolicy);
        this.securityPolicy = securityPolicy;
    }

    public String getSecurityPolicy() {
        return this.securityPolicy;
    }

    private void requireNotEmpty(String securityPolicy) {
        if (isEmptyOrNull(securityPolicy)) {
            throw new IllegalArgumentException("securityPolicy cannot be empty or null");
        }
    }

    public static boolean isEmptyOrNull(String securityPolicy) {
        return securityPolicy == null || securityPolicy.trim().isEmpty();
    }
}
