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

import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.internal.rest.configuration.FailureHandler;

public class ComponentConfigurationDTO implements Validable {

    private final String pid;
    private final OcdDTO definition;
    private final Map<String, PropertyDTO> properties;

    public ComponentConfigurationDTO(final String pid, final OcdDTO definition,
            final Map<String, PropertyDTO> properties) {
        this.pid = pid;
        this.definition = definition;
        this.properties = properties;
    }

    public String getPid() {
        return this.pid;
    }

    public Optional<OCD> getDefinition() {
        return Optional.ofNullable(this.definition);
    }

    public Map<String, PropertyDTO> getProperties() {
        return this.properties;
    }

    @Override
    public void validate() {
        FailureHandler.requireParameter(this.pid, "pid");
        FailureHandler.requireParameter(this.properties, "properties");

        for (final PropertyDTO param : properties.values()) {
            if (param == null) {
                throw FailureHandler.toWebApplicationException(Status.BAD_REQUEST, "propety values cannot be null");
            }

            param.validate();
        }
    }

}
