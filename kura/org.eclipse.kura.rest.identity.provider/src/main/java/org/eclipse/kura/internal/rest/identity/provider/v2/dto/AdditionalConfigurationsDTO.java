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

import org.eclipse.kura.rest.configuration.api.ComponentConfigurationDTO;

public class AdditionalConfigurationsDTO {

    private Set<ComponentConfigurationDTO> configurations = new HashSet<>();

    public void setConfigurations(Set<ComponentConfigurationDTO> configurations) {
        this.configurations = configurations;
    }

    public Set<ComponentConfigurationDTO> getConfigurations() {
        return this.configurations;
    }

    @Override
    public String toString() {
        return "AdditionalConfigurationDTO [configurations=" + this.configurations + "]";
    }

}
