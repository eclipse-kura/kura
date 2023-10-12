/*******************************************************************************
 * Copyright (c) 2021, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.configuration.api;

import java.util.List;
import java.util.stream.Collectors;

public class ComponentConfigurationList implements Validable {

    private final List<ComponentConfigurationDTO> configs;

    public ComponentConfigurationList(List<ComponentConfigurationDTO> configs) {
        this.configs = configs;
    }

    public List<ComponentConfigurationDTO> getConfigs() {
        return configs;
    }

    @Override
    public void validate() {
        FailureHandler.requireParameter(this.configs, "configs");
    }

    public ComponentConfigurationList replacePasswordsWithPlaceholder() {
        if (configs == null) {
            return this;
        }

        return new ComponentConfigurationList(
                configs.stream().map(ComponentConfigurationDTO::replacePasswordsWithPlaceholder)
                        .collect(Collectors.toList()));
    }
}
