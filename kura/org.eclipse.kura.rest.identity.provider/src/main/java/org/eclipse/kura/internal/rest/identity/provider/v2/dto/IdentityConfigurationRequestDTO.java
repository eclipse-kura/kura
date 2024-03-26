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
package org.eclipse.kura.internal.rest.identity.provider.v2.dto;

import java.util.HashSet;
import java.util.Set;

public class IdentityConfigurationRequestDTO {

    private IdentityDTO identity;
    private Set<String> configurationComponents = new HashSet<>();

    public void setIdentity(IdentityDTO identity) {
        this.identity = identity;
    }

    public IdentityDTO getIdentity() {
        return this.identity;
    }

    public void setConfigurationComponents(Set<String> configurationComponents) {
        this.configurationComponents = configurationComponents;
    }

    public Set<String> getConfigurationComponents() {
        return this.configurationComponents;
    }

    @Override
    public String toString() {
        return "IdentityConfigurationRequestDTO [identity=" + this.identity + ", configurationComponents="
                + this.configurationComponents + "]";
    }
}
