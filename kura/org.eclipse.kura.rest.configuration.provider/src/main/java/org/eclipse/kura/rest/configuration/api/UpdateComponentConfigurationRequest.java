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

import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.internal.rest.configuration.FailureHandler;

public class UpdateComponentConfigurationRequest implements Validable {

    private final List<ComponentConfigurationDTO> configs;
    private Boolean takeSnapshot;

    public UpdateComponentConfigurationRequest(List<ComponentConfigurationDTO> componentConfigurations,
            boolean takeSnapshot) {
        this.configs = componentConfigurations;
        this.takeSnapshot = takeSnapshot;
    }

    public List<ComponentConfigurationDTO> getComponentConfigurations() {
        return this.configs;
    }

    public boolean isTakeSnapshot() {
        return this.takeSnapshot == null || this.takeSnapshot;
    }

    @Override
    public void validate() {
        FailureHandler.requireParameter(this.configs, "configs");

        for (final ComponentConfigurationDTO config : this.configs) {
            if (config == null) {
                throw FailureHandler.toWebApplicationException(Status.BAD_REQUEST,
                        "component configuration objects cannot be null");
            }

            config.validate();
        }
    }
}
