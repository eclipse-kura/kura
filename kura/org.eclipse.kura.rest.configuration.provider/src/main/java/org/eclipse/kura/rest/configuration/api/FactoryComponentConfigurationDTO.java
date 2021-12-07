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

import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.eclipse.kura.internal.rest.configuration.FailureHandler;

public class FactoryComponentConfigurationDTO implements Validable {

    private final String factoryPid;
    private final String pid;
    private final Map<String, PropertyDTO> properties;

    public FactoryComponentConfigurationDTO(String factoryPid, String pid, Map<String, PropertyDTO> properties) {
        this.factoryPid = factoryPid;
        this.pid = pid;
        this.properties = properties;
    }

    public String getFactoryPid() {
        return this.factoryPid;
    }

    public String getPid() {
        return this.pid;
    }

    public Map<String, PropertyDTO> getProperties() {
        return this.properties != null ? this.properties : Collections.emptyMap();
    }

    @Override
    public void validate() {
        FailureHandler.requireParameter(this.factoryPid, "factoryPid");
        FailureHandler.requireParameter(this.pid, "pid");

        if (properties != null) {
            for (final PropertyDTO param : properties.values()) {
                if (param == null) {
                    throw FailureHandler.toWebApplicationException(Status.BAD_REQUEST, "propety values cannot be null");
                }

                param.validate();
            }
        }

    }
}
