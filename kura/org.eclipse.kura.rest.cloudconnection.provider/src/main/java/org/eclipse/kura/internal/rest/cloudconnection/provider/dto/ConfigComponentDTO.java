/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.cloudconnection.provider.dto;

import java.util.ArrayList;
import java.util.List;

public class ConfigComponentDTO {

    private List<ConfigParameterDTO> parameters;

    private String componentDescription;
    private String componentIcon;
    private String componentId;
    private String componentName;
    private String factoryPid;

    public ConfigComponentDTO(String componentDescription, String componentIcon, String componentId,
            String componentName, String factoryPid, List<ConfigParameterDTO> parameters) {
        this.componentDescription = componentDescription;
        this.componentIcon = componentIcon;
        this.componentId = componentId;
        this.componentName = componentName;
        this.factoryPid = factoryPid;
        this.parameters = parameters;
    }

    public ConfigComponentDTO(String componentDescription, String componentIcon, String componentId,
            String componentName, String factoryPid) {
        this(componentDescription, componentIcon, componentId, componentName, factoryPid, new ArrayList<>());
    }

    public List<ConfigParameterDTO> getParameters() {
        return this.parameters;
    }

    public String getComponentDescription() {
        return this.componentDescription;
    }

    public String getComponentIcon() {
        return this.componentIcon;
    }

    public String getComponentId() {
        return this.componentId;
    }

    public String getComponentName() {
        return this.componentName;
    }

    public String getFactoryId() {
        return this.factoryPid;
    }

}
