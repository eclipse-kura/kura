/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.inventory.resources;

import java.util.List;

public class DockerContainers {

    private List<DockerContainer> containers;

    public DockerContainers(List<DockerContainer> containers) {
        this.containers = containers;
    }

    public List<DockerContainer> getDockerContainers() {
        return this.containers;
    }

    public void setDockerContainers(List<DockerContainer> containers) {
        this.containers = containers;
    }

}
