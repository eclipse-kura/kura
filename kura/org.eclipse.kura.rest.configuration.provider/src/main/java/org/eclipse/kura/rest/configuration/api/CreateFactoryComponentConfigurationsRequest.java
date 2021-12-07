/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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

import org.eclipse.kura.internal.rest.configuration.FailureHandler;

public class CreateFactoryComponentConfigurationsRequest implements Validable {

    private final List<FactoryComponentConfigurationDTO> configs;
    private final boolean takeSnapshot;

    public CreateFactoryComponentConfigurationsRequest(List<FactoryComponentConfigurationDTO> configs,
            boolean takeSnapshot) {
        this.configs = configs;
        this.takeSnapshot = takeSnapshot;
    }

    public List<FactoryComponentConfigurationDTO> getConfigs() {
        return configs;
    }

    public boolean isTakeSnapshot() {
        return takeSnapshot;
    }

    @Override
    public void validate() {
        FailureHandler.requireParameter(this.configs, "configs");
    }
}
