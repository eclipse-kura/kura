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

    private List<ConfigParameterDTO> parameters = new ArrayList<>();

    private String componentDescription;
    private String componentIcon;
    private String componentId;
    private String componentName;
    private String factoryPid;
    private String driverPid;

    private boolean factoryComponent;
    private boolean wireComponent;
    private boolean driver;

    public ConfigComponentDTO() {
    }

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
        return parameters;
    }

    public void setParameters(List<ConfigParameterDTO> parameters) {
        this.parameters = parameters;
    }

    public String getComponentDescription() {
        return componentDescription;
    }

    public void setComponentDescription(String componentDescription) {
        this.componentDescription = componentDescription;
    }

    public String getComponentIcon() {
        return componentIcon;
    }

    public void setComponentIcon(String componentIcon) {
        this.componentIcon = componentIcon;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getFactoryPid() {
        return factoryPid;
    }

    public void setFactoryPid(String factoryPid) {
        this.factoryPid = factoryPid;
    }

    public boolean isFactoryComponent() {
        return factoryComponent;
    }

    public void setFactoryComponent(boolean factoryComponent) {
        this.factoryComponent = factoryComponent;
    }

    public boolean isWireComponent() {
        return wireComponent;
    }

    public void setWireComponent(boolean wireComponent) {
        this.wireComponent = wireComponent;
    }

    public boolean isDriver() {
        return driver;
    }

    public void setDriver(boolean driver) {
        this.driver = driver;
    }

    public String getDriverPid() {
        return driverPid;
    }

    public void setDriverPid(String driverPid) {
        this.driverPid = driverPid;
    }

}
