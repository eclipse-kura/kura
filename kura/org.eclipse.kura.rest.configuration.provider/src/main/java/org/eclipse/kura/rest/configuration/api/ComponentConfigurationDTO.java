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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.configuration.metatype.Scalar;
import org.eclipse.kura.request.handler.jaxrs.DefaultExceptionHandler;

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

    public ComponentConfigurationDTO replacePasswordsWithPlaceholder() {

        if (properties == null) {
            return this;
        }

        final Map<String, PropertyDTO> result = new HashMap<>(this.properties);

        for (final Entry<String, PropertyDTO> e : result.entrySet()) {
            e.setValue(replacePasswordsWithPlaceholder(e.getValue()));
        }

        return new ComponentConfigurationDTO(pid, definition, result);
    }

    public PropertyDTO replacePasswordsWithPlaceholder(final PropertyDTO property) {

        if (property == null || property.getType() != Scalar.PASSWORD) {
            return property;
        }

        if (property.getValue() instanceof String[]) {
            final String[] asStringArray = (String[]) property.getValue();
            final String[] result = new String[asStringArray.length];

            for (int i = 0; i < asStringArray.length; i++) {
                if (asStringArray[i] != null) {
                    result[i] = "placeholder";
                }
            }

            return new PropertyDTO(result, Scalar.PASSWORD);
        } else {
            return new PropertyDTO("placeholder", Scalar.PASSWORD);
        }
    }

    @Override
    public void validate() {
        FailureHandler.requireParameter(this.pid, "pid");
        FailureHandler.requireParameter(this.properties, "properties");

        for (final PropertyDTO param : properties.values()) {
            if (param == null) {
                throw DefaultExceptionHandler.buildWebApplicationException(Status.BAD_REQUEST,
                        "propety values cannot be null");
            }

            param.validate();
        }
    }

}
